package fi.pelam.ahma.serialization

import java.nio.charset.StandardCharsets._

import com.google.common.io.ByteSource
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {

  @Test(expected = classOf[IllegalArgumentException])
  def testReadFailNoRowId: Unit = {
    // no row types so error
    new TableReader(ByteSource.wrap("1,2,3,4,\n\n\n".getBytes(UTF_8))).read()
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testReadSimple: Unit = {
    // Works because row type identified
    val table = new TableReader(ByteSource.wrap("Comment,1,2,3,4\nComment\nComment,\n".getBytes(UTF_8))).read()
    assertEquals(RowType.Comment, table.getRowType(RowKey(0)))
    assertEquals(RowType.Comment, table.getRowType(RowKey(1)))
    assertEquals(RowType.Comment, table.getRowType(RowKey(2)))
    assertEquals(4, table.rowCount)
  }

  @Test
  def testGetRowAndColCount: Unit = {
    assertEquals((0, 0), TableReader.getRowAndColCount(List()))
    assertEquals((1, 1), TableReader.getRowAndColCount(List(CellKey(0, 0))))
    assertEquals((1, 2), TableReader.getRowAndColCount(List(CellKey(0, 1))))
    assertEquals((2, 1), TableReader.getRowAndColCount(List(CellKey(1, 0))))
    assertEquals((124, 457), TableReader.getRowAndColCount(List(CellKey(1, 0), CellKey(123, 456))))
  }

  @Test
  def testGetRowTypes {
  }

}
