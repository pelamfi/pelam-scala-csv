package fi.pelam.csv.table

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell.{RowKey, CellParsingError, CellParser, Cell}
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.SortedBiMap

object TableReader2 {

  type RowTyperResult[RT] = Either[TableReadingError, RT]

  type RowTyper[RT] = PartialFunction[(Cell), RowTyperResult[RT]]

  type ColTyperResult[CT] = Either[TableReadingError, CT]

  type RowTypes[RT] = SortedBiMap[RowKey, RT]

  type ColTyper[RT, CT] = PartialFunction[(Cell, RowTypes[RT]), ColTyperResult[CT]]

  type ColTypes[CT] = SortedBiMap[RowKey, CT]

  type CellUpgraderResult = Either[TableReadingError, Cell]

  type CellUpgrader[RT, CT, M <: TableMetadata] = PartialFunction[(Cell, M, CellType[RT, CT]), CellUpgraderResult]
}

case class TableReadingErrors(phase: Int = 0, errors: IndexedSeq[TableReadingError] = IndexedSeq()) {
  def noErrors = errors.isEmpty 
}

// TODO: Sketching and hacking replacement for TableReader...
class TableReader2[RT, CT, M <: TableMetadata](
  val openInputStream: () => java.io.InputStream,
  val metadata: M = SimpleTableMetadata(),
  val rowTyper: TableReader2.RowTyper[RT] = PartialFunction.empty,
  val colTyper: TableReader2.ColTyper[RT, CT] = PartialFunction.empty,
  val cellUpgrader: TableReader2.CellUpgrader[RT, CT, M] = PartialFunction.empty
  ) {

  import TableReader2._

  type ResultTable = Table[RT, CT, M]

  case class TableReadingState(cells: IndexedSeq[Cell] = IndexedSeq(),
    rowTypes: RowTypes[RT] = SortedBiMap(),
    colTypes: ColTypes[CT] = SortedBiMap(),
    errors: TableReadingErrors = TableReadingErrors()) {
  }

  sealed trait TableReadingPhase {
    def map(f: TableReadingState => TableReadingState): TableReadingPhase
    def flatMap(inner: TableReadingState => TableReadingPhase): TableReadingPhase
    def run(inputState: TableReadingState): TableReadingState
  }

  case class Phase(phaseFunc: TableReadingState => TableReadingState) extends TableReadingPhase {
    def map(mapFunc: TableReadingState => TableReadingState) = Phase{ state: TableReadingState => mapFunc(phaseFunc(state)) }

    def flatMap(inner: TableReadingState => TableReadingPhase): TableReadingPhase = PhaseFlatmapped { state: TableReadingState =>
      val result = phaseFunc(state)
      if (result.errors.noErrors) {
        inner(result) // to call the inner func, we need state and we get the wrapper,
      } else {
        // Don't run further phases if one had error
        FailedPhase(state)
      }
    }

    def run(inputState: TableReadingState) = phaseFunc(inputState)
  }

  case class PhaseFlatmapped(phaseFunc: TableReadingState => TableReadingPhase) extends TableReadingPhase {
    def map(mapFunc: TableReadingState => TableReadingState) = ???

    def flatMap(inner: TableReadingState => TableReadingPhase): TableReadingPhase = ???

    def run(inputState: TableReadingState) = inputState
  }

  case class FailedPhase(lastState: TableReadingState) extends TableReadingPhase {
    def map(f2: TableReadingState => TableReadingState) = this

    def flatMap(inner: TableReadingState => TableReadingPhase): TableReadingPhase = this

    override def run(inputState: TableReadingState): TableReadingState = ???
  }

  def phase1(process: TableReadingState): TableReadingState = {
    process
  }

  def phase2(process: TableReadingState): TableReadingState = {
    process
  }

  def phase3(process: TableReadingState): TableReadingState = {
    process
  }

  def read(): (ResultTable, TableReadingErrors) = {

    val phases = for (_ <- Phase(phase1);
                      _ <- Phase(phase2);
                      x <- Phase(phase3)) yield x

    val initial = TableReadingState()

    val result = phases.run(initial)

    // TODO: Redesign Table class...
    (null /* Table(metadata, result.cells, result.rowTypes, result.colTypes) */ , result.errors)
  }
}

/**
 * This class is part of the the higher level api for reading, writing and processing CSV data.
 *
 * [[Table]] is an immutable data structure for holding and processing
 * the parsed data in a spreadsheet program like format.
 *
 * [[TableWriter]] is the counterpart of this class for writing [[Table]] out to disk.
 *
 * == Stages ==
 * The table reading process may fail and terminate at each phase. Then an incomplete Table object
 * will be returned containing the errors detected so far.
 *
 * The table reading is split to phases to allow implementing format detection heuristics in that
 * lock some variables during the earlier phases and then proceeding to later phases.
 *
 * Locking some variables and then proceeding results in more efficient algorithm
 * than exhaustive search of the full set of combinations (character set, locale, separator etc).
 *
 *   - Parse to cells
 *   - Detect cell types
 *   - Upgrade cells
 *
 * @param openInputStream
 * @param rowTypeDefinition
 * @param colTypeDefinition
 * @param cellTypes map from [[CellType]] to [[fi.pelam.csv.cell.CellParser CellParser]] instances. Use this to get more
 *                  specialized [[fi.pelam.csv.cell.Cell Cell]] instances than the simple
 *                  [[fi.pelam.csv.cell.StringCell StringCell]].
 * @param locales
 * @tparam RT
 * @tparam CT
 */
// TODO: Docs, better names
// TODO: Finish documenting the phases and the detection idea after it is implemented
class TableReader[RT, CT](val openInputStream: () => java.io.InputStream,
  val rowTypeDefinition: TableReader.RowTyper[RT, CT] = PartialFunction.empty,
  val colTypeDefinition: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
  val cellTypes: TableReader.CellUpgrades[RT, CT] = PartialFunction.empty,
  val locales: Seq[Locale] = Seq(Locale.ROOT)
  ) {

  import TableReader._

  var cells: IndexedSeq[Cell] = IndexedSeq()

  def read(): Table[RT, CT, SimpleTableMetadata] = {

    // TODO: Charset detection (try UTF16 and iso8859)
    val charset = StandardCharsets.UTF_8

    val inputStream = openInputStream()

    try {

      val inputReader: java.io.Reader = new BufferedReader(new java.io.InputStreamReader(inputStream, charset), 1024)

      // TODO: Separator detection
      val csvSeparator = CsvConstants.defaultSeparatorChar

      val csvParser = new CsvReader(inputReader, separator = csvSeparator)

      this.cells = csvParser.raiseOnError.toIndexedSeq

      val detectedCellTypes: CellTypes[RT, CT] = detectCellTypeLocaleAndRowTypes()

      val dataLocale = detectDataLocaleAndUpgradeCells(detectedCellTypes)

      // Return final product
      Table(charset, csvSeparator, dataLocale, detectedCellTypes, cells)

    } finally {
      inputStream.close()
    }
  }

  def detectCellTypeLocaleAndRowTypes(): CellTypes[RT, CT] = {

    val bestTypes = locales.foldLeft[Option[CellTypes[RT, CT]]](None)
    { (prev: Option[CellTypes[RT, CT]], locale) =>
      if (prev.map(_.errors == 0).getOrElse(false)) {
        // TODO: Naming of everything here...

        // All row types are now identified. Consider cellTypeLocale to be properly detected.
        // Detection heuristic ends on first zero errors solution as an optimization
        prev
      } else {
        val types = buildCellTypes(cells, locale, rowTypeDefinition, colTypeDefinition)

        // Prefer to report the shortest list of errors
        prev match {
          case None => Some(types)
          case Some(prevBest) if prevBest.errors.size > types.errors.size => Some(types)
          case Some(prevBest) => Some(prevBest)
        }
      }
    }

    // Throw if errors
    bestTypes.fold(sys.error("Could not detect locale. No loclaes defined?"))( types =>
      if (types.errors.size > 0) {
        val message = "Failed to identify language and/or some row and column types.\n" +
          types.errors.foldLeft("")(_ + _ + "\n")

        sys.error(message)
      } else {
        types
      }
    )
  }

  def upgradeCell(detectedCellTypes: CellTypes[RT, CT], cell: Cell, locale: Locale): Either[TableReadingError, Cell] = {
    val upgraded = for (cellType: CellType[RT, CT] <- detectedCellTypes.getCellType(cell);
                        cellParser <- cellTypes.lift(cellType)) yield {

      val result = cellParser.parse(cell.cellKey, locale, cell.serializedString)

      result match {
        // Add cell and cell type to possible error message
        case Left(error: CellParsingError) => Left(TableReadingError(error, cell, cellType))
        case Right(cell) => Right(cell)
      }
    }

    // Handle no cell type defined case
    upgraded.getOrElse(Right(cell))
  }

  def detectDataLocaleAndUpgradeCells(cellTypes: CellTypes[RT, CT]): Locale = {

    // Guess first the already detected cellTypeLocale, if that fails try english.
    // This is a way to limit combinations.
    val reorderedLocales = Seq(cellTypes.locale) ++ locales.diff(Seq(cellTypes.locale))

    val perLocaleResults = for (locale <- reorderedLocales) yield {

      val cellsUpgradedOrErrors = for (cell <- cells) yield {
        upgradeCell(cellTypes, cell, locale)
      }

      // http://stackoverflow.com/a/26579082/1148030
      val result =
        cellsUpgradedOrErrors.foldRight(CellUpgradeAndLocaleResults(locale))(
          (e, r) => e.fold(error => r.copy(errors = r.errors :+ error), cell => r.copy(cells = r.cells :+ cell)))

      result
    }

    // Pick the one with no errors or least number of errors (in case of failure)
    val bestResult = perLocaleResults.sortWith((a, b) => a.errors.size - b.errors.size < 0).head

    if (bestResult.errors.isEmpty) {
      cells = bestResult.cells
      bestResult.locale
    } else {
      val message = "Failed to parse data in some cells and or identify language/locale.\n" +
        bestResult.errors.foldLeft("")(_ + _ + "\n")

      sys.error(message)
    }

  }

}

object TableReader {
  type TyperOutput[T] = Either[TableReadingError, T]

  type RowTyper[RT, CT] = PartialFunction[(Cell, CellTypes[RT, CT]), TyperOutput[RT]]

  type ColTyper[RT, CT] = PartialFunction[(Cell, CellTypes[RT, CT]), TyperOutput[CT]]

  type CellUpgrades[RT, CT] = PartialFunction[CellType[RT, CT], CellParser]

  case class CellUpgradeAndLocaleResults(locale: Locale,
    errors: IndexedSeq[TableReadingError] = IndexedSeq(),
    cells: IndexedSeq[Cell] = IndexedSeq())

  def detectRowTypes[RT, CT](rowTypeDefinition: ((Cell, CellTypes[RT, CT])) => Option[TyperOutput[RT]],
    cell: Cell,
    cellTypes: CellTypes[RT, CT]): CellTypes[RT, CT] = {

    if (cellTypes.rowTypes.isDefinedAt(cell.rowKey)) {
      cellTypes
    } else {
      rowTypeDefinition(cell, cellTypes) match {
        case Some(Left(e)) => cellTypes.copy[RT, CT](errors = cellTypes.errors :+ e.addedDetails(cell))
        case Some(Right(rowType)) => cellTypes.copy[RT, CT](rowTypes = cellTypes.rowTypes.updated(cell.rowKey, rowType))
        case None => cellTypes
      }
    }
  }

  def detectColTypes[RT, CT](colTypeDefinition: ((Cell, CellTypes[RT, CT])) => Option[TyperOutput[CT]],
    cell: Cell,
    cellTypes: CellTypes[RT, CT]): CellTypes[RT, CT] = {

    if (cellTypes.colTypes.isDefinedAt(cell.colKey)) {
      cellTypes
    } else {
      colTypeDefinition(cell, cellTypes) match {
        case Some(Left(e)) => cellTypes.copy[RT, CT](errors = cellTypes.errors :+ e.addedDetails(cell))
        case Some(Right(colType)) => cellTypes.copy[RT, CT](colTypes = cellTypes.colTypes.updated(cell.colKey, colType))
        case None => cellTypes
      }
    }
  }

  def buildCellTypes[RT, CT](cells: TraversableOnce[Cell], locale: Locale,
    rowTypeDefinition: TableReader.RowTyper[RT, CT],
    colTypeDefinition: TableReader.ColTyper[RT, CT]): CellTypes[RT, CT] = {

    val initial = CellTypes[RT, CT](locale = locale)

    val rowTypeDefinitionLifted = rowTypeDefinition.lift
    val colTypeDefinitionLifted = colTypeDefinition.lift

    // For each cell try to use xTypeDefinition functions to identify column and row types
    // unless they are already identified.
    //
    // Also collect errors detected by those functions to support CSV format detection heuristic.
    cells.foldLeft[CellTypes[RT, CT]](initial) { (cellTypes: CellTypes[RT, CT], cell: Cell) =>
      try {
        detectColTypes(colTypeDefinitionLifted, cell, detectRowTypes(rowTypeDefinitionLifted, cell, cellTypes))
      } catch {
        case e: Exception => throw new RuntimeException("Error processing cell " + cell, e)
      }
    }
  }

}
