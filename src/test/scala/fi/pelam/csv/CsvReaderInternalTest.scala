package fi.pelam.csv

import java.io.StringReader

import org.junit.Test
import org.junit.Assert._


/**
 * NOTE: [[CsvReaderInternal]] is mostly tested indirectly through its wrapper's test  [[CsvReaderTest]].
 */
class CsvReaderInternalTest {

  @Test
  def testRead: Unit = {
    val reader = new CsvReaderInternal(new StringReader("foo,bar\n"))
    assertEquals(Some(Right(StringCell(CellKey(0,0), "foo"))), reader.read())
    assertEquals(Some(Right(StringCell(CellKey(0,1), "bar"))), reader.read())
    assertEquals(None, reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }
}