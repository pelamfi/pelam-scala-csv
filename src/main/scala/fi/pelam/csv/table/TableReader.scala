package fi.pelam.csv.table

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell._
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.SortedBiMap

object TableReader2 {

  type RowTyperResult[RT] = Either[TableReadingError, RT]

  type RowTyper[RT] = PartialFunction[(Cell), RowTyperResult[RT]]

  type ColTyperResult[CT] = Either[TableReadingError, CT]

  type RowTypes[RT] = SortedBiMap[RowKey, RT]

  type ColTyper[RT, CT] = PartialFunction[(Cell, RowTypes[RT]), ColTyperResult[CT]]

  type ColTypes[CT] = SortedBiMap[ColKey, CT]

  type CellUpgraderResult = Either[TableReadingError, Cell]

  type CellUpgrader[RT, CT, M <: TableMetadata] = PartialFunction[(Cell, M, CellType[RT, CT]), CellUpgraderResult]
}

case class TableReadingErrors(phase: Int = 0, errors: IndexedSeq[TableReadingError] = IndexedSeq()) {
  def noErrors = errors.isEmpty 
}

case class TableReadingState[RT, CT](cells: IndexedSeq[Cell] = IndexedSeq(),
  rowTypes: TableReader2.RowTypes[RT] = SortedBiMap[RowKey, RT](),
  colTypes: TableReader2.ColTypes[CT] = SortedBiMap[ColKey, CT](),
  errors: TableReadingErrors = TableReadingErrors()) {
}

sealed trait TableReadingPhase[RT, CT] {
  def map(f: TableReadingState[RT, CT] => TableReadingState[RT, CT]): TableReadingPhase[RT, CT]
  def flatMap(inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT]
  def run(inputState: TableReadingState[RT, CT]): TableReadingState[RT, CT]
}

case class Phase[RT, CT](phaseFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) extends TableReadingPhase[RT, CT] {

  def map(mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = Phase[RT, CT](state => mapFunc(phaseFunc(state)))

  def flatMap(inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT] = PhaseFlatmap(this, inner)

  def run(inputState: TableReadingState[RT, CT]) = phaseFunc(inputState)
}

case class PhaseFlatmap[RT, CT](outer: TableReadingPhase[RT, CT],
  inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT],
  mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT] = (state: TableReadingState[RT, CT]) => state) extends TableReadingPhase[RT, CT] {

  def map(newMapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = PhaseFlatmap(outer, inner,
    (state: TableReadingState[RT, CT]) => mapFunc(newMapFunc(state)))

  def flatMap(newInner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT] = PhaseFlatmap(this, newInner)

  def run(inputState: TableReadingState[RT, CT]) = {
    val outerResult = outer.run(inputState)
    if (outerResult.errors.noErrors) {
      mapFunc(inner(outerResult).run(outerResult)) // to call the inner func, we need state and we get the wrapper,
    } else {
      // Don't run further phases if one had error
      mapFunc(outerResult)
    }
  }
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

  type State = TableReadingState[RT, CT]

  def phase1(state: State): State = {
    state
  }

  def phase2(state: State): State = {
    state
  }

  def phase3(state: State): State = {
    state
  }

  def read(): (ResultTable, TableReadingErrors) = {

    val phases = for (_ <- Phase(phase1);
                      _ <- Phase(phase2);
                      x <- Phase(phase3)) yield x

    val initial = TableReadingState[RT, CT]()

    val result = phases.run(initial)

    val cellTypes = CellTypes[RT, CT](result.rowTypes, result.colTypes)

    (Table(metadata, cellTypes, result.cells), result.errors)
  }
}

case class CellTypesTemp[RT, CT](cellTypes: CellTypes[RT, CT] = CellTypes[RT, CT](),
  errors: Seq[TableReadingError] = IndexedSeq(),
  locale: Locale) {

  def rowTypes = cellTypes.rowTypes

  def colTypes = cellTypes.colTypes
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

  def read(): Table[RT, CT, LocaleTableMetadata] = {

    // TODO: Charset detection (try UTF16 and iso8859)
    val charset = StandardCharsets.UTF_8

    val inputStream = openInputStream()

    try {

      val inputReader: java.io.Reader = new BufferedReader(new java.io.InputStreamReader(inputStream, charset), 1024)

      // TODO: Separator detection
      val csvSeparator = CsvConstants.defaultSeparatorChar

      val csvParser = new CsvReader(inputReader, separator = csvSeparator)

      this.cells = csvParser.throwOnError.toIndexedSeq

      val cellTypesTemp = detectCellTypeLocaleAndRowTypes()

      val dataLocale = detectDataLocaleAndUpgradeCells(cellTypesTemp)

      // Return final product
      Table(LocaleTableMetadata(dataLocale, cellTypesTemp.locale, charset, csvSeparator), cellTypesTemp.cellTypes, cells)

    } finally {
      inputStream.close()
    }
  }

  def detectCellTypeLocaleAndRowTypes(): CellTypesTemp[RT, CT] = {

    val bestTypes = locales.foldLeft[Option[CellTypesTemp[RT, CT]]](None) { (prev: Option[CellTypesTemp[RT, CT]], locale) =>
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
    bestTypes.fold(sys.error("Could not detect locale. No loclaes defined?"))(types =>
      if (types.errors.size > 0) {
        val message = "Failed to identify language and/or some row and column types.\n" +
          types.errors.foldLeft("")(_ + _ + "\n")

        sys.error(message)
      } else {
        types
      }
    )
  }

  def upgradeCell(detectedCellTypes: CellTypesTemp[RT, CT], cell: Cell, locale: Locale): Either[TableReadingError, Cell] = {
    val upgraded = for (cellType: CellType[RT, CT] <- detectedCellTypes.cellTypes.getCellType(cell);
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

  def detectDataLocaleAndUpgradeCells(cellTypes: CellTypesTemp[RT, CT]): Locale = {

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

  type RowTyper[RT, CT] = PartialFunction[(Cell, CellTypesTemp[RT, CT]), TyperOutput[RT]]

  type ColTyper[RT, CT] = PartialFunction[(Cell, CellTypesTemp[RT, CT]), TyperOutput[CT]]

  type CellUpgrades[RT, CT] = PartialFunction[CellType[RT, CT], CellParser]

  case class CellUpgradeAndLocaleResults(locale: Locale,
    errors: IndexedSeq[TableReadingError] = IndexedSeq(),
    cells: IndexedSeq[Cell] = IndexedSeq())

  def detectRowTypes[RT, CT](rowTypeDefinition: ((Cell, CellTypesTemp[RT, CT])) => Option[TyperOutput[RT]],
    cell: Cell,
    cellTypes: CellTypesTemp[RT, CT]): CellTypesTemp[RT, CT] = {

    if (cellTypes.rowTypes.isDefinedAt(cell.rowKey)) {
      cellTypes
    } else {
      rowTypeDefinition(cell, cellTypes) match {
        case Some(Left(e)) => cellTypes.copy[RT, CT](errors = cellTypes.errors :+ e.addedDetails(cell))
        case Some(Right(rowType)) => cellTypes.copy[RT, CT](cellTypes =
          cellTypes.cellTypes.copy(rowTypes = cellTypes.rowTypes.updated(cell.rowKey, rowType)))
        case None => cellTypes
      }
    }
  }

  def detectColTypes[RT, CT](colTypeDefinition: ((Cell, CellTypesTemp[RT, CT])) => Option[TyperOutput[CT]],
    cell: Cell,
    cellTypes: CellTypesTemp[RT, CT]): CellTypesTemp[RT, CT] = {

    if (cellTypes.colTypes.isDefinedAt(cell.colKey)) {
      cellTypes
    } else {
      colTypeDefinition(cell, cellTypes) match {
        case Some(Left(e)) => cellTypes.copy[RT, CT](errors = cellTypes.errors :+ e.addedDetails(cell))
        case Some(Right(colType)) =>
          cellTypes.copy[RT, CT](cellTypes = cellTypes.cellTypes.copy(colTypes = cellTypes.colTypes.updated(cell.colKey, colType)))
        case None => cellTypes
      }
    }
  }

  def buildCellTypes[RT, CT](cells: TraversableOnce[Cell], locale: Locale,
    rowTypeDefinition: TableReader.RowTyper[RT, CT],
    colTypeDefinition: TableReader.ColTyper[RT, CT]): CellTypesTemp[RT, CT] = {

    val initial = CellTypesTemp[RT, CT](locale = locale)

    val rowTypeDefinitionLifted = rowTypeDefinition.lift
    val colTypeDefinitionLifted = colTypeDefinition.lift

    // For each cell try to use xTypeDefinition functions to identify column and row types
    // unless they are already identified.
    //
    // Also collect errors detected by those functions to support CSV format detection heuristic.
    cells.foldLeft[CellTypesTemp[RT, CT]](initial) { (cellTypes: CellTypesTemp[RT, CT], cell: Cell) =>
      try {
        detectColTypes(colTypeDefinitionLifted, cell, detectRowTypes(rowTypeDefinitionLifted, cell, cellTypes))
      } catch {
        case e: Exception => throw new RuntimeException("Error processing cell " + cell, e)
      }
    }
  }

}
