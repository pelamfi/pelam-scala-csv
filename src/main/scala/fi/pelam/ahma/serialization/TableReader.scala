package fi.pelam.ahma.serialization

import java.nio.charset.StandardCharsets
import java.util.Locale

import com.google.common.io.ByteSource
import fi.pelam.ahma.localization.AhmaLocalization
import grizzled.slf4j.Logging

import scala.collection.immutable.TreeMap
import scala.collection.{SortedMap, mutable}

class TableReader(input: ByteSource) extends Logging {

  var colTypes: SortedMap[ColKey, ColType] = SortedMap()

  var locale: Locale = Locale.ROOT

  var rowTypes: SortedMap[RowKey, RowType] = SortedMap()

  var cells: mutable.Buffer[StringCell] = mutable.Buffer()

  var errors: Option[Seq[String]] = None

  import fi.pelam.ahma.serialization.TableReader._

  def read(): Table = {

    // TODO: Charset detection (try UTF16 and iso8859)
    val inputString = input.asCharSource(StandardCharsets.UTF_8).read()

    // TODO: Separator detection
    this.cells = parseSimpleCells(',', inputString)

    detectStringLocaleAndRowTypes()

    detectNumberLocaleAndUpgradeCells()

    val table = new Table(locale, rowTypes, colTypes, cells)

    table
  }

  def detectStringLocaleAndRowTypes(): Unit = {

    for (stringLocale <- locales) {

      val (rowTypes, rowErrors) = getRowTypes(cells, stringLocale)

      val columnHeaderRow = rowTypes.find(_._2 == RowType.ColumnHeader)

      val (colTypes, colErrors) = if (columnHeaderRow.isDefined) {
        getColTypes(cells, columnHeaderRow.get._1, stringLocale)
      } else {
        (TreeMap[ColKey, ColType](), List("No row marked to contain column headers found."))
      }

      val errors = rowErrors ++ colErrors

      if (errors.size == 0) {
        // All row types identified, Consider stringLocale detected
        this.locale = stringLocale
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

  def detectNumberLocaleAndUpgradeCells(): Unit = {

  }


}

object TableReader {
  val locales = List(AhmaLocalization.localeEn, AhmaLocalization.localeFi)

  def parseSimpleCells(separator: Char, input: String): mutable.Buffer[StringCell] = {
    new CsvParser(input, separator = separator).parse()
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
                      if cell.rowKey == headerRow && cell.colKey != Table.rowTypeCol) yield {

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
