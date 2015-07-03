package fi.pelam.csv

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets._
import java.util.Locale

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Resources}
import org.junit.Assert._
import org.junit.Test
import TestRowType._
import TestColType._

import scala.collection.immutable.SortedMap

class TableReaderTest {
  val headerAndCommentsOnly = "ColumnHeader,CommentCol,CommentCol,CommentCol,CommentCol\n" +
    "CommentRow,1,2,3,4\n" +
    "CommentRow\n" +
    "CommentRow,\n"

  val rowAndColTypesFiDataEn = "CommentRow,1,2,3,4\n" +
    "ColumnHeader,Qualifications,WorkerId,IntParam1,Salary\n" +
    "Worker,ValueCC,4001,8,\"12,000.00\"\n"

  val commentsOnlyFi = "CommentRow,1,2,3,4\nCommentRow\nCommentRow,\n"

  val noRowTypes = "1,2,3,4,\nCommentRow\n\n"

  implicit def opener(string: String): () => java.io.InputStream = {
    () => new ByteArrayInputStream(string.getBytes(Charsets.UTF_8))
  }

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }

  @Test(expected = classOf[RuntimeException])
  def testReadFailNoRowId: Unit = {
    // no row types so error
    new TableReader(noRowTypes, rowTypes, colTypes).read()
  }

  @Test
  def testJustReadSimple: Unit = {
    // Works because row type identified
    new TableReader(headerAndCommentsOnly).read()
  }

  @Test
  def testRowCount: Unit = {
    val table = new TableReader(headerAndCommentsOnly).read()
    assertEquals(4, table.rowCount)
  }

  @Test
  def testColCount: Unit = {
    val table = new TableReader(headerAndCommentsOnly).read()
    assertEquals(5, table.colCount)
  }

  @Test
  def testRowTypeFi: Unit = {
    val table = new TableReader(headerAndCommentsOnly, rowTypes, colTypes).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testRowType: Unit = {
    val table = new TableReader(headerAndCommentsOnly, rowTypes, colTypes).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testParseLocalizedNumbersAndQuotes: Unit = {
    val input = "Title,Salary,BoolParam1\nWorker,\"12,000.00\",TRUE\n"
    val table = new TableReader(input).read()
    val cells = table.getCells(RowKey(1))
    assertEquals("Worker\n12,000.00\nTRUE\n", cells.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testUpgradeCellType: Unit = {
    val table = new TableReader[TestRowType, TestColType](rowAndColTypesFiDataEn, rowTypes, colTypes,
      Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell), List(Locales.localeEn, Locales.localeFi)).read()

    val cells = table.getSingleCol(TestColType.Salary, TestRowType.Worker)

    assertEquals(Locales.localeEn, table.dataLocale)

    val expectedIntegerCell = IntegerCell.fromString(CellKey(2, 4), Locales.localeEn, "12000").right.get

    assertEquals(IndexedSeq(expectedIntegerCell), cells)
  }

  @Test
  def testUpgradeCellTypeParsingFailed: Unit = {
    val brokenData = rowAndColTypesFiDataEn.replace("\"12,000.00\"", "injected-error-should-be-number")

    try {
      new TableReader[TestRowType, TestColType](brokenData, rowTypes, colTypes,
        Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell)).read()
      fail()
    } catch {
      case e: Exception => {
        assertEquals("Failed to parse data in some cells and or identify language/locale.\n" +
          "TableReadingError(Expected integer, but input 'injected-error-should-be-number' could not be fully parsed with locale fi at Row 2, Column E (4) CellType(Worker,Salary))\n", e.getMessage())
      }
    }
  }

  val rowTypes: TableReader.RowTyper[TestRowType, TestColType] = {
    case (cell, _) if cell.colKey.index == 0 => {
      TestRowType.namesToValuesMap.get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown row type."))
      }
    }
  }

  val colTypes: TableReader.ColTyper[TestRowType, TestColType] = {
    case (cell, _) if cell.colKey.index == 0 => Right(TestColType.RowType)
    case (cell, types) if types.rowTypes.get(cell.rowKey) == Some(TestRowType.ColumnHeader) => {
      TestColType.namesToValuesMap.get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown column type."))
      }
    }
  }

  @Test
  def testBuildCellTypesRow: Unit = {
    assertEquals(CellTypes(
      rowTypes = BiMap(SortedMap(RowKey(0) -> TestRowType.CommentRow)),
      cellTypesLocale = Locale.ROOT),
      TableReader.buildCellTypes(List(StringCell(CellKey(0, 0), "CommentRow")), Locale.ROOT, rowTypes, PartialFunction.empty))
  }

  @Test
  def testBuildCellTypesCol: Unit = {
    assertEquals(CellTypes(
      rowTypes = BiMap(SortedMap(RowKey(0) -> TestRowType.ColumnHeader)),
      colTypes = BiMap(SortedMap(ColKey(0) -> TestColType.RowType)),
      cellTypesLocale = Locale.ROOT),
      TableReader.buildCellTypes(List(StringCell(CellKey(0, 0), "ColumnHeader")), Locale.ROOT, rowTypes, colTypes))
  }

  @Test
  def testBuildCellTypesError: Unit = {
    val cell = StringCell(CellKey(0, 0), "Bogus")
    assertEquals(CellTypes(
      errors = IndexedSeq(TableReadingError("Unknown row type.", Some(cell))),
      cellTypesLocale = Locale.ROOT),
      TableReader.buildCellTypes(List(cell), Locale.ROOT, rowTypes, PartialFunction.empty))
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    val table = new TableReader(rowAndColTypesFiDataEn, rowTypes, colTypes).read()
    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary), table.colTypes.values.toList)
    assertEquals(List(TestRowType.CommentRow, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val table = new TableReader(file, rowTypes, colTypes).read()

    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary,
      BoolParam1, PrevWeek, PrevWeek, PrevWeek), table.colTypes.values.toList.slice(0, 9))

    assertEquals(List(CommentRow, CommentRow, ColumnHeader, Day, Worker, Worker),
      table.rowTypes.values.toList.slice(0, 6))

    assertEquals("Qualifications for all workers should match.", "MSc/MSP,BSc,MBA",
      table.getSingleCol(Qualifications, Worker).map(_.serializedString).reduce(_ + "," + _))

  }

}
