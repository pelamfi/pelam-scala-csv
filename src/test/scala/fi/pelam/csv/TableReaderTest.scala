package fi.pelam.csv

import java.nio.charset.StandardCharsets._
import java.util.Locale

import com.google.common.io.{ByteSource, Resources}
import org.junit.Assert._
import org.junit.Test
import TestRowType._
import TestColType._

class TableReaderTest {
  val headerAndCommentsOnly = ByteSource.wrap("Header,Comment,Comment,Comment,Comment\nComment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))

  val rowAndColTypesFiDataEn = ByteSource.wrap(("Comment,1,2,3,4\n" +
    "Title,Tyypit,WorkerId,IntParam1,Salary\n" +
    "Worker,ValueCC,4001,8,\"12,000.00\"\n").getBytes(UTF_8))

  val commentsOnlyFi = ByteSource.wrap("Comment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))

  val noRowTypes = ByteSource.wrap("1,2,3,4,\nComment\n\n".getBytes(UTF_8))

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }

  @Test(expected = classOf[RuntimeException])
  def testReadFailNoRowId: Unit = {
    // no row types so error
    new TableReader(noRowTypes).read()
  }

  @Test
  def testJustReadSimple: Unit = {
    // Works because row type identified
    new TableReader(headerAndCommentsOnly).read()
  }

  @Test
  def testRowCount: Unit = {
    val table = new TableReader[Unit, Unit](headerAndCommentsOnly, Map()).read()
    assertEquals(4, table.rowCount)
  }

  @Test
  def testColCount: Unit = {
    val table = new TableReader[Unit, Unit](headerAndCommentsOnly, Map()).read()
    assertEquals(5, table.colCount)
  }

  @Test
  def testRowTypeFi: Unit = {
    val table = new TableReader[Unit, Unit](headerAndCommentsOnly, Map()).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testRowType: Unit = {
    val table = new TableReader[Unit, Unit](headerAndCommentsOnly, Map()).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testParseLocalizedNumbersAndQuotes: Unit = {
    val input = ByteSource.wrap("Title,Salary,BoolParam1\nWorker,\"12,000.00\",TRUE\n".getBytes(UTF_8))
    val table = new TableReader[Unit, Unit](input, Map()).read()
    val cells = table.getCells(RowKey(1))
    assertEquals("Worker\n12,000.00\nTRUE\n", cells.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testUpgradeCellType: Unit = {
    val table = new TableReader[TestRowType, TestColType](rowAndColTypesFiDataEn,
      Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell)).read()

    val cells = table.getSingleCol(TestColType.Salary, TestRowType.Worker)

    assertEquals(Locales.localeEn, table.dataLocale)

    val expectedIntegerCell = IntegerCell.fromString(CellKey(2, 4), Locales.localeEn, "12000").right.get

    assertEquals(IndexedSeq(expectedIntegerCell), cells)
  }

  @Test
  def testUpgradeCellTypeParsingFailed: Unit = {
    val rowAndColTypesFiDataEn = ByteSource.wrap(
      ("Title,Tyypit,WorkerId,IntParam1,Salary\n" +
        "Worker,ValueCC,4001,8,injected-error-should-be-number\n").getBytes(UTF_8))

    try {
      new TableReader(rowAndColTypesFiDataEn,
        Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell)).read()
      fail()
    } catch {
      case e: Exception => {
        assertEquals("Failed to parse data in some cells and or identify language/locale.\n" +
          "TableReadingError(Expected integer, but input 'injected-error-should-be-number' could not be fully parsed with locale fi at Row 2, Column E (4) CellType(Worker,Salary))\n", e.getMessage())
      }
    }
  }

  @Test
  def testGetRowTypes: Unit = {
    assertEquals((Map(RowKey(0) -> TestRowType.CommentRow), Seq()),
      TableReader.detectCellTypes(List(StringCell(CellKey(0, 0), "Comment")), Locale.ROOT, TableReader.emptyRowTypeDefinition[TestRowType, TestColType], TableReader.emptyColTypeDefinition[TestRowType, TestColType]))
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    val table = new TableReader[TestRowType, TestColType](rowAndColTypesFiDataEn, Map()).read()
    assertEquals(List(RowHeader, Qualifications, WorkerId, IntParam1, Salary), table.colTypes.values.toList)
    assertEquals(List(TestRowType.CommentRow, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val table = new TableReader[TestRowType, TestColType](file, Map()).read()

    assertEquals(List(RowHeader, Qualifications, WorkerId, IntParam1, CommentCol, Salary,
      BoolParam1, IntParam2, PrevWeek, PrevWeek, PrevWeek), table.colTypes.values.toList.slice(0, 11))

    assertEquals(List(CommentRow, CommentRow, ColumnHeader, Day, Worker, Worker), table.rowTypes.values.toList.slice(0, 6))

    assertEquals("ValueAA/ValueBB/ValueCC", table.getSingleCol(Qualifications, Worker)(3).serializedString)
  }

}
