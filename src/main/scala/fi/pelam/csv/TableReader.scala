package fi.pelam.csv

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

class TableReader[RT, CT](openInputStream: () => java.io.InputStream,
  rowTypeDefinition: TableReader.RowTypeDefinition[RT, CT] = TableReader.emptyRowTypeDefinition[RT, CT],
  colTypeDefinition: TableReader.ColTypeDefinition[RT, CT] = TableReader.emptyColTypeDefinition[RT, CT],
  cellTypes: TableReader.CellFactories[RT, CT] = Map[CellType[RT, CT], CellFactory]()
  ) {

  import TableReader._

  var dataLocale: Locale = Locale.ROOT

  var cells: IndexedSeq[Cell] = IndexedSeq()

  // TODO: Naming
  var detectedCellTypes: Option[TypesFoo[RT, CT]] = None

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
      Table(charset, csvSeparator, detected.cellTypesLocale, dataLocale, detected.rowTypes, detected.colTypes, cells)

    } finally {
      inputStream.close()
    }
  }

  def detectCellTypeLocaleAndRowTypes(): Unit = {

    for (cellTypeLocale <- locales) {

      // TODO: Naming of everything here...

      val types = detectCellTypes(cells, cellTypeLocale, rowTypeDefinition, colTypeDefinition)

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
        types.errors.foldLeft("")(_ + _.msg + "\n")

      sys.error(message)
    }
  }

  def upgradeCell(cell: Cell, locale: Locale): Either[TableReadingError, Cell] = {
    val upgraded = for (cellType: CellType[RT, CT] <- getCellType(cell);
         factory <- cellTypes.lift(cellType)) yield {

      val result = factory.fromString(cell.cellKey, locale, cell.serializedString)

      result match {
        // Add cell type to possible error message
        case Left(error) => Left(error.copy(msg = error.msg + s" $cellType"))
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

  def getCellType(cell: Cell): Option[CellType[RT, CT]] = {
    for(rowType <- getRowType(cell);
        colType <- getColType(cell)) yield {
      CellType(rowType, colType)
    }
  }

  def getColType(cell: Cell): Option[CT] = {
    for (cellTypes <- detectedCellTypes;
         colType <- cellTypes.colTypes.get(cell.colKey)) yield colType
  }

  def getRowType(cell: Cell): Option[RT] = {
    for (cellTypes <- detectedCellTypes;
         rowType <- cellTypes.rowTypes.get(cell.rowKey)) yield rowType
  }
}

object TableReader {
  case class TypesFoo[RT, CT](
    rowTypes: SortedMap[RowKey, RT] = SortedMap[RowKey, RT](),
    colTypes: SortedMap[ColKey, CT] = SortedMap[ColKey, CT](),
    errors: Seq[TableReadingError] = IndexedSeq(),
    cellTypesLocale: Locale
    )

  // TODO: Name
  type TypeDefinitionOutput[T] = Option[Either[TableReadingError, T]]

  type RowTypeDefinition[RT, CT] = (Cell, TypesFoo[RT, CT]) => TypeDefinitionOutput[RT]

  type ColTypeDefinition[RT, CT] = (Cell, TypesFoo[RT, CT]) => TypeDefinitionOutput[CT]

  def emptyRowTypeDefinition[RT, CT]: RowTypeDefinition[RT, CT] = (_, _) => None

  def emptyColTypeDefinition[RT, CT]: ColTypeDefinition[RT, CT] = (_, _) => None

  // TODO: Move elsewhere
  val localeEn: Locale = Locale.forLanguageTag("EN")

  // TODO: Names of these things, CellFactories, cellTypes, CellFactory
  type CellFactories[RT, CT] = PartialFunction[CellType[RT, CT], CellFactory]

  // TODO: Make locale candidate list a parameter
  val locales = List(localeEn)

  case class CellUpgradeAndLocaleResults(locale: Locale,
    errors: IndexedSeq[TableReadingError] = IndexedSeq(),
    cells: IndexedSeq[Cell] = IndexedSeq())

  // type rowTypeDefinition[RT] = (TraversableOnce[Cell], Locale) => (SortedMap[RowKey, RT], Seq[String])

  def detectCellTypes[RT, CT](cells: TraversableOnce[Cell], locale: Locale,
    rowTypeDefinition: TableReader.RowTypeDefinition[RT, CT],
    colTypeDefinition: TableReader.ColTypeDefinition[RT, CT]): TypesFoo[RT, CT] = {

    val initial = TypesFoo[RT, CT](cellTypesLocale = locale)

    // For each cell try to use xTypeDefinition functions to identify column and row types
    // unless they are already identified.
    //
    // Also collect errors detected by those functions to support CSV format detection heuristic.
    cells.foldLeft[TypesFoo[RT, CT]](initial) { (t: TypesFoo[RT, CT], cell: Cell) =>

      val definition = for (definition <- rowTypeDefinition(cell, t);
                            if (!t.rowTypes.isDefinedAt(cell.rowKey))) yield definition

      val updatedT: TypesFoo[RT, CT] = definition match {
        case Some(Left(e)) => t.copy[RT, CT](errors = t.errors :+ e)
        case Some(Right(rowType)) => t.copy[RT, CT](rowTypes = t.rowTypes.updated(cell.rowKey, rowType))
        case None => t
      }

      val definition2 = for (definition2 <- colTypeDefinition(cell, updatedT);
                             if (!t.colTypes.isDefinedAt(cell.colKey))) yield definition2

      val updatedT2: TypesFoo[RT, CT] = definition2 match {
        case Some(Left(e)) => t.copy[RT, CT](errors = t.errors :+ e)
        case Some(Right(colType)) => t.copy[RT, CT](colTypes = t.colTypes.updated(cell.colKey, colType))
        case None => t
      }

      updatedT2
    }
  }

}
