package fi.pelam.csv

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.collection.SortedMap

class TableReader[RT, CT](openInputStream: () => java.io.InputStream,
  rowTypeDefinition: TableReader.RowTyper[RT, CT] = PartialFunction.empty,
  colTypeDefinition: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
  cellTypes: TableReader.CellUpgrades[RT, CT] = PartialFunction.empty
  ) {

  import TableReader._

  var dataLocale: Locale = Locale.ROOT

  var cells: IndexedSeq[Cell] = IndexedSeq()

  // TODO: Naming
  var detectedCellTypes: Option[CellTypes[RT, CT]] = None

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

      detectCellTypeLocaleAndRowTypes()

      detectDataLocaleAndUpgradeCells()

      val detected = detectedCellTypes.get

      // Return final product
      Table(charset, csvSeparator, dataLocale, detected, cells)

    } finally {
      inputStream.close()
    }
  }

  def detectCellTypeLocaleAndRowTypes(): Unit = {

    for (cellTypeLocale <- locales) {

      // TODO: Naming of everything here...

      val types = buildCellTypes(cells, cellTypeLocale, rowTypeDefinition, colTypeDefinition)

      if (types.errors.size == 0) {
        // All row types are now identified. Consider cellTypeLocale to be properly detected.
        this.detectedCellTypes = Some(types)

        // Detection heuristic ends on first zero errors solution as an optimization
        return
      } else {
        // Prefer to report the shortest list of errors
        this.detectedCellTypes = this.detectedCellTypes match {
          case None => Some(types)
          case Some(prevBest) if prevBest.errors.size > types.errors.size => Some(types)
          case Some(prevBest) => Some(prevBest)
        }
      }
    }

    // Throw if errors
    for (types <- this.detectedCellTypes;
         if types.errors.size > 0) {

      val message = "Failed to identify language and/or some row names in the first column.\n" +
        types.errors.foldLeft("")(_ + _ + "\n")

      sys.error(message)
    }
  }

  def upgradeCell(cell: Cell, locale: Locale): Either[TableReadingError, Cell] = {
    val upgraded = for (detectedCellTypes <- detectedCellTypes;
                        cellType: CellType[RT, CT] <- detectedCellTypes.getCellType(cell);
         factory <- cellTypes.lift(cellType)) yield {

      val result = factory.fromString(cell.cellKey, locale, cell.serializedString)

      result match {
        // Add cell and cell type to possible error message
        case Left(error: TableReadingError) => Left(error.addedDetails(cell, cellType))
        case cell => cell
      }
    }

    // Handle no cell type defined case
    upgraded.getOrElse(Right(cell))
  }

  def detectDataLocaleAndUpgradeCells(): Unit = {

    // TODO: Make locale candidate list a parameter
    // Guess first the already detected cellTypeLocale, if that fails try english.
    // This is a way to limit combinations.
    val dataLocaleCandidates = List(localeEn)

    val perLocaleResults = for (locale <- dataLocaleCandidates) yield {

      val cellsUpgradedOrErrors = for (cell <- cells) yield {
        upgradeCell(cell, locale)
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
      this.dataLocale = bestResult.locale
      cells = bestResult.cells
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

  // TODO: Move elsewhere
  val localeEn: Locale = Locale.forLanguageTag("EN")

  type CellUpgrades[RT, CT] = PartialFunction[CellType[RT, CT], CellUpgrade]

  // TODO: Make locale candidate list a parameter
  val locales = List(localeEn)

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

    val initial = CellTypes[RT, CT](cellTypesLocale = locale)

    val rowTypeDefinitionLifted = rowTypeDefinition.lift
    val colTypeDefinitionLifted = colTypeDefinition.lift

    // For each cell try to use xTypeDefinition functions to identify column and row types
    // unless they are already identified.
    //
    // Also collect errors detected by those functions to support CSV format detection heuristic.
    cells.foldLeft[CellTypes[RT, CT]](initial) { (cellTypes: CellTypes[RT, CT], cell: Cell) =>
      detectColTypes(colTypeDefinitionLifted, cell, detectRowTypes(rowTypeDefinitionLifted, cell, cellTypes))
    }
  }

}
