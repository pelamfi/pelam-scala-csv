package fi.pelam.ahma.serialization

import java.nio.charset.StandardCharsets._
import java.util.Locale

import com.google.common.io.{ByteSource, Resources}
import fi.pelam.ahma.serialization.ColType._
import fi.pelam.ahma.serialization.RowType._
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {

  val headerAndCommentsOnly = ByteSource.wrap("Header\nComment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))

  val rowAndColTypesFi = ByteSource.wrap(("Comment,1,2,3,4\nTitle,Tyypit,WorkerId,IntParam1,TimeParam1\n" +
    "Worker,ValueCC,4001\n").getBytes(UTF_8))

  val commentsOnlyFi = ByteSource.wrap("Comment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))

  val noRowTypes = ByteSource.wrap("1,2,3,4,\nComment\n\n".getBytes(UTF_8))

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
    val table = new TableReader(headerAndCommentsOnly).read()
    assertEquals(ColumnHeader, table.getRowType(RowKey(0)))
    assertEquals(Comment, table.getRowType(RowKey(1)))
    assertEquals(Comment, table.getRowType(RowKey(2)))
  }

  @Test
  def testRowType: Unit = {
    val table = new TableReader(headerAndCommentsOnly).read()
    assertEquals(ColumnHeader, table.getRowType(RowKey(0)))
    assertEquals(Comment, table.getRowType(RowKey(1)))
    assertEquals(Comment, table.getRowType(RowKey(2)))
  }

  @Test
  def testParseSimpleCells: Unit = {
    assertEquals("SimpleCell(CellKey(0,0),Comment)\n" +
      "SimpleCell(CellKey(0,1),1)\n" +
      "SimpleCell(CellKey(1,0),Comment)\n" +
      "SimpleCell(CellKey(1,1),2)\n", TableReader.parseSimpleCells(',',
      IndexedSeq("Comment,1", "Comment,2")).fold("")("" + _ + _ + "\n"))
  }

  @Test
  def testGetRowTypes: Unit = {
    assertEquals((Map(RowKey(0) -> RowType.Comment), Seq()),
      TableReader.getRowTypes(List(SimpleCell(CellKey(0, 0), "Comment")), Locale.ROOT))
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    val table = new TableReader(rowAndColTypesFi).read()
    assertEquals(List(Types, WorkerId, MaxWorkRun, TimeParam1), table.colTypes.values.toList)
    assertEquals(List(Comment, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))
    val table = new TableReader(file).read()
    assertEquals(List(Types, WorkerId, MaxWorkRun, TimeParam1), table.colTypes.values.toList)
    assertEquals(List(Comment, ColumnHeader, Worker), table.rowTypes.values.toList)

  }

}
