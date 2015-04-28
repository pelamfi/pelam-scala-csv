package fi.pelam.ahma.serialization

import java.nio.charset.StandardCharsets._
import java.util.Locale

import com.google.common.io.{ByteSource, Resources}
import fi.pelam.ahma.serialization.ColType._
import fi.pelam.ahma.serialization.RowType._
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {

  val headerAndCommentsOnly = ByteSource.wrap("Header,Comment,Comment,Comment,Comment\nComment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))

  val rowAndColTypesFi = ByteSource.wrap(("Comment,1,2,3,4\nTitle,Tyypit,WorkerId,IntParam1,TimeParam1\n" +
    "Worker,ValueCC,4001\n").getBytes(UTF_8))

  val commentsOnlyFi = ByteSource.wrap("Comment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))

  val noRowTypes = ByteSource.wrap("1,2,3,4,\nComment\n\n".getBytes(UTF_8))

  @Test(expected = classOf[RuntimeException])
  def testReadFailNoRowId: Unit = {
    // no row types so error
    new TableReader(noRowTypes, Map()).read()
  }

  @Test
  def testJustReadSimple: Unit = {
    // Works because row type identified
    new TableReader(headerAndCommentsOnly, Map()).read()
  }

  @Test
  def testRowCount: Unit = {
    val table = new TableReader(headerAndCommentsOnly, Map()).read()
    assertEquals(4, table.rowCount)
  }

  @Test
  def testColCount: Unit = {
    val table = new TableReader(headerAndCommentsOnly, Map()).read()
    assertEquals(5, table.colCount)
  }

  @Test
  def testRowTypeFi: Unit = {
    val table = new TableReader(headerAndCommentsOnly, Map()).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testRowType: Unit = {
    val table = new TableReader(headerAndCommentsOnly, Map()).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testParseLocalizedNumbersAndQuotes: Unit = {
    val input = ByteSource.wrap("Title,TimeParam1,BoolParam1\nWorker,\"12,000.00\",TRUE\n".getBytes(UTF_8))
    val table = new TableReader(input, Map()).read()
    val cells = table.getCells(RowKey(1))
    assertEquals("Worker\n12,000.00\nTRUE\n", cells.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseSimpleCells: Unit = {

    val parsed = TableReader.parseSimpleCells(',', "Comment,1\nComment,2")

    val expected = "Cell containing 'Comment' at Row 1, Column A (0)\n" +
      "Cell containing '1' at Row 1, Column B (1)\n" +
      "Cell containing 'Comment' at Row 2, Column A (0)\n" +
      "Cell containing '2' at Row 2, Column B (1)\n"

    assertEquals(expected, parsed.foldLeft("")(_ + _ + "\n"))
  }

  @Test
  def testUpgradeCellType: Unit = ???

  @Test
  def testUpgradeCellTypeParsingFailed: Unit = ???

  @Test
  def testGetRowTypes: Unit = {
    assertEquals((Map(RowKey(0) -> RowType.CommentRow), Seq()),
      TableReader.getRowTypes(List(StringCell(CellKey(0, 0), "Comment")), Locale.ROOT))
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    val table = new TableReader(rowAndColTypesFi, Map()).read()
    assertEquals(List(RowHeader, Types, WorkerId, MaxWorkRun, TimeParam1), table.colTypes.values.toList)
    assertEquals(List(RowType.CommentRow, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val table = new TableReader(file, Map()).read()

    assertEquals(List(RowHeader, Types, WorkerId, MaxWorkRun, CommentCol, TimeParam1,
      SundayWorkPreferred, Week1FreeDays, Week2FreeDays, History, History), table.colTypes.values.toList.slice(0, 11))

    assertEquals(List(CommentRow, CommentRow, ColumnHeader, Day, Worker, Worker), table.rowTypes.values.toList.slice(0, 6))

    // table.getCol(Types, Worker)
  }

}
