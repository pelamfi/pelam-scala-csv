package fi.pelam.ahma.serialization

import java.nio.charset.StandardCharsets
import java.util.Locale

import com.google.common.io.ByteSource
import fi.pelam.ahma.localization.AhmaLocalization
import grizzled.slf4j.Logging

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

class TableReader(input: ByteSource, cellTypes: CellTypes.CellTypeMap) extends Logging {

  var colTypes: SortedMap[ColKey, ColType] = SortedMap()

  var cellTypeLocale: Locale = Locale.ROOT

  var dataLocale: Locale = Locale.ROOT

  var rowTypes: SortedMap[RowKey, RowType] = SortedMap()

  var cells: IndexedSeq[Cell] = IndexedSeq()

  var errors: Option[Seq[String]] = None

  import fi.pelam.ahma.serialization.TableReader._

  def read(): Table = {

    // TODO: Charset detection (try UTF16 and iso8859)
    val inputString = input.asCharSource(StandardCharsets.UTF_8).read()

    // TODO: Separator detection
    this.cells = parseSimpleCells(',', inputString).toIndexedSeq

    detectStringLocaleAndRowTypes()

    detectDataLocaleAndUpgradeCells()

    val table = new Table(cellTypeLocale, rowTypes, colTypes, cells)

    table
  }

  def detectStringLocaleAndRowTypes(): Unit = {

    for (cellTypeLocale <- locales) {

      val (rowTypes, rowErrors) = getRowTypes(cells, cellTypeLocale)

      val columnHeaderRow = rowTypes.find(_._2 == RowType.ColumnHeader)

      val (colTypes, colErrors) = if (columnHeaderRow.isDefined) {
        getColTypes(cells, columnHeaderRow.get._1, cellTypeLocale)
      } else {
        (TreeMap[ColKey, ColType](), List("No row marked to contain column headers found."))
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

  def detectDataLocaleAndUpgradeCells(): Unit = {

    // Guess first the already detected cellTypeLocale, if that fails try english.
    // This is a way to limit combinations.
    val dataLocaleCandidates = List(cellTypeLocale, AhmaLocalization.localeEn)

    val perLocaleResults = for (locale <- dataLocaleCandidates) yield {

      val cellsUpgradedOrErrors = for (cell <- cells) yield {
        val typeKey: (RowType, ColType) = getCellTypeKey(cell)

        val upgradedOrError = cellTypes.get(typeKey)
          .map(_.fromString(cell.cellKey, locale, cell.serializedString))
          .getOrElse(Right(cell))

        upgradedOrError
      }

      // http://stackoverflow.com/a/26579082/1148030
      val result =
        cellsUpgradedOrErrors.foldRight(CellUpgradeAndLocaleResults(locale))(
          (e, r) => e.fold(error => r.copy(errors = r.errors :+ error), cell => r.copy(cells = r.cells :+ cell)))

      result
    }

    val bestResult = perLocaleResults.sortWith((a, b) => a.errors.size - b.errors.size < 0).head

    if (bestResult.errors.isEmpty) {
      this.dataLocale = bestResult.locale
      cells = bestResult.cells
    }

  }

  def getCellTypeKey(cell: Cell) = (getRowType(cell), getColType(cell))

  def getColType(cell: Cell): ColType = colTypes.getOrElse(cell.colKey, ColType.CommentCol)

  def getRowType(cell: Cell): RowType = rowTypes.getOrElse(cell.rowKey, RowType.CommentRow)
}

object TableReader {

  val locales = List(AhmaLocalization.localeEn, AhmaLocalization.localeFi)

  case class CellUpgradeAndLocaleResults(locale: Locale,
    errors: IndexedSeq[TableReadingError] = IndexedSeq(),
    cells: IndexedSeq[Cell] = IndexedSeq())

  def parseSimpleCells(separator: Char, input: String): IndexedSeq[Cell] = {
    new CsvParser(input, separator = separator).parse().toIndexedSeq
  }

  def getRowTypes(cells: TraversableOnce[Cell], locale: Locale): (SortedMap[RowKey, RowType], Seq[String]) = {

    val errors = Seq.newBuilder[String]

    val rowTypeReverseMap = AhmaLocalization.getEnumMap(locale, RowType)

    val result = for (cell <- cells;
                      if cell.colKey == Table.rowTypeCol) yield {

      val rowTypeOption = rowTypeReverseMap.getReverse(cell.serializedString)

      if (rowTypeOption.isDefined) {
        cell.rowKey -> rowTypeOption.get
      } else {
        errors += s"Unknown row type '${cell.serializedString}' in language '${locale.getDisplayName()}'"
        cell.rowKey -> RowType.CommentRow
      }

    }

    (TreeMap[RowKey, RowType]() ++ result, errors.result)
  }

  def getColTypes(cells: TraversableOnce[Cell], headerRow: RowKey, locale: Locale): (SortedMap[ColKey, ColType], Seq[String]) = {

    val errors = Seq.newBuilder[String]

    val colTypeReverseMap = AhmaLocalization.getEnumMap(locale, ColType)

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

    (TreeMap[ColKey, ColType]() ++ result, errors.result)
  }


}
