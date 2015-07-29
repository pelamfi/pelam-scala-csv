package fi.pelam.csv.table

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell.{CellParsingError, CellDeserializer, Cell}
import fi.pelam.csv.stream.CsvReader

/**
 * This class is part of the the higher level api for reading, writing and processing CSV data.
 *
 * [[Table]] is an immutable data structure for holding and processing
 * the parsed data in a spreadsheet program like format.
 *
 * [[TableWriter]] is the counterpart of this class for writing [[Table]] out to disk.
 *
 * @param openInputStream
 * @param rowTypeDefinition
 * @param colTypeDefinition
 * @param cellTypes map from [[CellType]] to [[CellDeserializer]] instances. Use this to get more
 *                  specialized [[fi.pelam.csv.cell.Cell Cell]] instances than the simple
 *                  [[fi.pelam.csv.cell.StringCell StringCell]].
 * @param locales
 * @tparam RT
 * @tparam CT
 */
// TODO: Docs, better names
class TableReader[RT, CT](val openInputStream: () => java.io.InputStream,
  val rowTypeDefinition: TableReader.RowTyper[RT, CT] = PartialFunction.empty,
  val colTypeDefinition: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
  val cellTypes: TableReader.CellUpgrades[RT, CT] = PartialFunction.empty,
  val locales: Seq[Locale] = Seq(Locale.ROOT)
  ) {

  import TableReader._

  var cells: IndexedSeq[Cell] = IndexedSeq()

  def read(): Table[RT, CT] = {

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
                        factory <- cellTypes.lift(cellType)) yield {

      val result = factory.deserialize(cell.cellKey, locale, cell.serializedString)

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

  type CellUpgrades[RT, CT] = PartialFunction[CellType[RT, CT], CellDeserializer]

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
