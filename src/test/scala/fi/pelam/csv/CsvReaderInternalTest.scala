package fi.pelam.csv

import java.io.StringReader

import fi.pelam.csv.cell.{CellKey, StringCell}
import org.junit.Test
import org.junit.Assert._


/**
 * @note [[CsvReaderInternal]] is mostly tested indirectly through its wrapper's tests in [[CsvReaderTest]].
 */
class CsvReaderInternalTest {

  @Test
  def testRead: Unit = {
    val reader = new CsvReaderInternal(new StringReader("fooxbar\n"), 'x')
    assertEquals(Some(Right(StringCell(CellKey(0,0), "foo"))), reader.read())
    assertEquals(Some(Right(StringCell(CellKey(0,1), "bar"))), reader.read())
    assertEquals(None, reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }

  @Test
  def testReadEmpty: Unit = {
    val reader = new CsvReaderInternal(new StringReader(""), 'x')
    assertEquals(None, reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }

  @Test
  def testReadError: Unit = {
    val reader = new CsvReaderInternal(new StringReader("\"\n"), 'x')
    assertEquals(Some(Left(CsvReaderError("Unclosed quote", CellKey(0, 0)))), reader.read())
    assertEquals("Subsequent calls give None", None, reader.read())
  }
}