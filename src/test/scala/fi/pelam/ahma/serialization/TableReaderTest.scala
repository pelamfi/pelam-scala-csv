package fi.pelam.ahma.serialization

import java.nio.charset.StandardCharsets._

import com.google.common.io.ByteSource
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {

  @Test
  def testRead: Unit = {
    new TableReader(ByteSource.wrap("1,2,3,4,\n\n\n".getBytes(UTF_8)))
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
