package fi.pelam.csv

import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

class TableReader[RT, CT](inputSource: () => java.io.Reader, cellTypes: TableReader.CellFactories[RT, CT]) {

  import TableReader._

  var colTypes: SortedMap[ColKey, CT] = SortedMap()

  var cellTypeLocale: Locale = Locale.ROOT

  var dataLocale: Locale = Locale.ROOT

  var rowTypes: SortedMap[RowKey, RT] = SortedMap()

  var cells: IndexedSeq[Cell] = IndexedSeq()

  var errors: Option[Seq[String]] = None

  def read(): Table[RT, CT] = {

    // TODO: Charset detection (try UTF16 and iso8859)
    val charset = StandardCharsets.UTF_8

    val inputReader: java.io.Reader = inputSource()

    // TODO: Separator detection
    val csvSeparator = CsvConstants.defaultSeparatorChar

    val csvParser = new CsvReader(inputReader, separator = csvSeparator)

    this.cells = csvParser.raiseOnError.toIndexedSeq

    detectCellTypeLocaleAndRowTypes()

    detectDataLocaleAndUpgradeCells()

    val table = Table(charset, csvSeparator, cellTypeLocale, dataLocale, rowTypes, colTypes, cells)

    table
  }

  def detectCellTypeLocaleAndRowTypes(): Unit = {

    for (cellTypeLocale <- locales) {

      val (rowTypes, rowErrors) = getRowTypes(cells, cellTypeLocale)

      val columnHeaderRow = rowTypes.find(_._2 == RT.ColumnHeader)

      val (colTypes, colErrors) = if (columnHeaderRow.isDefined) {
        getColTypes(cells, columnHeaderRow.get._1, cellTypeLocale)
      } else {
        (TreeMap[ColKey, CT](), List("No row marked to contain column headers found."))
      }

      val errors = rowErrors ++ colErrors

      if (errors.size == 0) {
        // All row types are now identified. Consider cellTypeLocale to be properly detected.
        this.cellTypeLocale = cellTypeLocale
        this.rowTypes = rowTypes
        this.colTypes = colTypes
        this.errors = None
        return
      } else {
        // Prefer to report the shortest list of errors
        if (this.errors.map(_.size > errors.size).getOrElse(true)) {
          this.errors = Some(errors)
        }
      }
    }

    val message = "Failed to identify language and/or some row names in the first column.\n" +
      errors.map(_.fold("")(_ + _ + "\n")).getOrElse("")

    error(message)

    sys.error(message)
  }

  def upgradeCell(cell: Cell, locale: Locale): Either[TableReadingError, Cell] = {
    for (cellType <- getCellType(cell)) yield {
      if (cellTypes.isDefinedAt(cellType)) {
        val factory = cellTypes(cellType)
        val result = factory.fromString(cell.cellKey, locale, cell.serializedString)

        // Add cell type to possible error message
        result.fold(error => Left(error.copy(msg = error.msg + s" $cellType")), Right(_))
      } else {
        // Unchanged
        Right(cell)
      }
    }
  }

  def detectDataLocaleAndUpgradeCells(): Unit = {

    // TODO: Make locale candidate list a parameter
    // Guess first the already detected cellTypeLocale, if that fails try english.
    // This is a way to limit combinations.
    val dataLocaleCandidates = List(cellTypeLocale, AhmaLocalization.localeEn)

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

      error(message)

      sys.error(message)
    }

  }

  def getCellType(cell: Cell): Option[CellType[RT, CT]] = {
    for(rowType <- getRowType(cell);
        colType <- getColType(cell)) yield {
      CellType(rowType, colType)
    }
  }

  def getColType(cell: Cell): Option[CT] = colTypes.get(cell.colKey)

  def getRowType(cell: Cell): Option[RT] = rowTypes.get(cell.rowKey)
}

object TableReader {
  // TODO: Move elsewhere
  val localeEn: Locale = Locale.forLanguageTag("EN")

  // TODO: Names of these things, CellFactories, cellTypes, CellFactory
  type CellFactories[RT, CT] = PartialFunction[CellType[RT, CT], CellFactory]

  // TODO: Make locale candidate list a parameter
  val locales = List(localeEn)

  case class CellUpgradeAndLocaleResults(locale: Locale,
    errors: IndexedSeq[TableReadingError] = IndexedSeq(),
    cells: IndexedSeq[Cell] = IndexedSeq())

  def getRowTypes[RT, CT](cells: TraversableOnce[Cell], locale: Locale): (SortedMap[RowKey, RT], Seq[String]) = {

    val errors = Seq.newBuilder[String]

    // TODO: Make row type and this localized row type name map a parameter
    val rowTypeReverseMap = AhmaLocalization.getEnumMap(locale, RT)

    val result = for (cell <- cells;
                      if cell.colKey == Table.rowTypeCol) yield {

      val rowTypeOption = rowTypeReverseMap.getReverse(cell.serializedString)

      if (rowTypeOption.isDefined) {
        cell.rowKey -> rowTypeOption.get
      } else {
        errors += s"Unknown row type '${cell.serializedString}' in language '${locale.getDisplayName()}'"
        cell.rowKey -> RT.CommentRow
      }

    }

    (TreeMap[RowKey, RT]() ++ result, errors.result)
  }

  def getColTypes[RT, CT](cells: TraversableOnce[Cell], headerRow: RowKey, locale: Locale): (SortedMap[ColKey, CT], Seq[String]) = {

    val errors = Seq.newBuilder[String]

    // TODO: Make row type and this localized row type name map a parameter
    val colTypeReverseMap = AhmaLocalization.getEnumMap(locale, CT)

    val result = for (cell <- cells;
                      if cell.rowKey == headerRow) yield {

      val colTypeOption = colTypeReverseMap.getReverse(cell.serializedString)

      if (colTypeOption.isDefined) {
        cell.colKey -> colTypeOption.get
      } else {
        errors += s"Unknown column type '${cell.serializedString}' in language '${locale.getDisplayName()}'"
        cell.colKey -> null
      }

    }

    (TreeMap[ColKey, CT]() ++ result, errors.result)
  }


}