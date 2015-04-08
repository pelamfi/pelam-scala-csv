package fi.pelam.ahma.serialization

import java.nio.charset.{Charset, StandardCharsets}
import java.util.Locale

import com.google.common.io.ByteSource
import fi.pelam.ahma.localization.AhmaLocalization
import grizzled.slf4j.Logging

import scala.io.Source

class TableReader(input: ByteSource) extends Logging {

  var colTypes: Map[ColKey, ColType] = Map()

  var locale: Locale = Locale.ROOT

  var rowTypes: Map[RowKey, RowType] = Map()

  var cells: IndexedSeq[SimpleCell] = IndexedSeq()

  var errors: Option[Seq[String]] = None

  import fi.pelam.ahma.serialization.TableReader._

  def read(): Table = {

    // TODO: Charset detection (try UTF16 and iso8859)
    val lines = getLines(input, StandardCharsets.UTF_8)

    // TODO: Separator detection
    this.cells = parseSimpleCells(',', lines)

    detectLocaleAndRowTypes()

    val table = new Table(rowTypes, colTypes)

    table.addCells(cells)

    table
  }

  def detectLocaleAndRowTypes(): Unit = {

    for (locale <- locales) {

      val (rowTypes, rowErrors) = getRowTypes(cells, locale)

      val columnHeaderRow = rowTypes.find(_._2 == RowType.ColumnHeader)

      val (colTypes, colErrors) = if (columnHeaderRow.isDefined) {
        getColTypes(cells, columnHeaderRow.get._1, locale)
      } else {
        (Map[ColKey, ColType](), List("No row marked to contain column headers found."))
      }

      val errors = rowErrors ++ colErrors

      if (errors.size == 0) {
        // All row types identified, Consider locale detected
        this.locale = locale
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


}

object TableReader {
  val locales = List(AhmaLocalization.localeEn, AhmaLocalization.localeFi)

  def getLines(input: ByteSource, encoding: Charset) = {
    // Bypassing the codec handling in scala.io but using it to extract lines
    val source = Source.fromString(input.asCharSource(StandardCharsets.UTF_8).read())

    val lines = source.getLines().toIndexedSeq

    lines
  }

  def parseSimpleCells(separator: Char, lines: IndexedSeq[String]): IndexedSeq[SimpleCell] = {
    val cells = for (line <- lines.zipWithIndex) yield {
      for (cell <- line._1.split(separator).zipWithIndex) yield {
        val key = new CellKey(line._2, cell._2)
        new SimpleCell(key, cell._1)
      }
    }

    cells.flatten.toIndexedSeq
  }

  def getRowTypes(cells: TraversableOnce[Cell], locale: Locale): (Map[RowKey, RowType], Seq[String]) = {

    val errors = Seq.newBuilder[String]

    val rowTypeMap = AhmaLocalization.getReverseMap(locale, "RowType", RowType.values.map(_.toString))

    val result = for (cell <- cells;
                      if cell.colKey == Table.rowTypeCol) yield {

      val rowTypeString = rowTypeMap.get(cell.serializedString)

      if (rowTypeString.isDefined) {
        val rowType = RowType.namesToValuesMap(rowTypeString.get)
        cell.rowKey -> rowType
      } else {
        errors += s"Unknown row type '${cell.serializedString}' in language '${locale.getDisplayName()}'"
        cell.rowKey -> RowType.Comment
      }

    }

    (result.toMap, errors.result)
  }

  def getColTypes(cells: TraversableOnce[Cell], headerRow: RowKey, locale: Locale): (Map[ColKey, ColType], Seq[String]) = {

    val errors = Seq.newBuilder[String]

    val colTypeMap = AhmaLocalization.getReverseMap(locale, "ColType", ColType.values.map(_.toString))

    val result = for (cell <- cells;
                      if cell.rowKey == headerRow && cell.colKey != Table.rowTypeCol) yield {

      val colTypeString = colTypeMap.get(cell.serializedString)

      if (colTypeString.isDefined) {
        val colType = ColType.namesToValuesMap(colTypeString.get)
        cell.colKey -> colType
      } else {
        errors += s"Unknown column type '${cell.serializedString}' in language '${locale.getDisplayName()}'"
        cell.colKey -> null
      }

    }

    (result.toMap, errors.result)
  }


}
