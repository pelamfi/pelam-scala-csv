package fi.pelam.csv

import com.google.common.base.Charsets
import com.google.common.io.{CharStreams, Resources}
import org.junit.Assert._
import org.junit.Test

class CsvWriterTest {

  val stringBuilder = new java.lang.StringBuilder()

  val charWriter = CharStreams.asWriter(stringBuilder)

  val csvWriter = new CsvWriter(charWriter)

  @Test(expected = classOf[RuntimeException])
  def testWriteSameKeyError: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 0), "bar")))
  }

  @Test(expected = classOf[RuntimeException])
  def testWriteOldKeyError: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 1), "bar"), StringCell(CellKey(0, 0), "bar")))
  }

  @Test
  def testSingleRow: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 1), "bar")))
    assertEquals("foo,bar", stringBuilder.toString())
  }

  @Test
  def testFinalNewLine: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 1), "bar")))
    csvWriter.goToNextRow()
    assertEquals("foo,bar\n", stringBuilder.toString())
  }

  @Test
  def testTwoLines: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 1), "bar")))
    csvWriter.write(List(StringCell(CellKey(1, 0), "x"), StringCell(CellKey(1, 1), "y")))
    assertEquals("foo,bar\nx,y", stringBuilder.toString())
  }

  @Test
  def testTwoLinesWithGoToNextLine: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 1), "bar")))
    csvWriter.goToNextRow()
    csvWriter.write(List(StringCell(CellKey(1, 0), "x"), StringCell(CellKey(1, 1), "y")))
    csvWriter.goToNextRow()
    assertEquals("foo,bar\nx,y\n", stringBuilder.toString())
  }

  @Test
  def testSkips: Unit = {
    csvWriter.write(StringCell(CellKey(0, 0), "foo"))
    csvWriter.write(StringCell(CellKey(1, 1), "bar"))
    assertEquals("foo\n,bar", stringBuilder.toString())
  }

  @Test
  def testQuotes: Unit = {
    assertEquals("foo", CsvWriter.serialize(StringCell(CellKey(0, 0), "foo"), ','))
    assertEquals("\"f,oo\"", CsvWriter.serialize(StringCell(CellKey(0, 0), "f,oo"), ','))
    assertEquals("\"f,,oo\"", CsvWriter.serialize(StringCell(CellKey(0, 0), "f,,oo"), ','))
    assertEquals("\"f\"\"oo\"", CsvWriter.serialize(StringCell(CellKey(0, 0), "f\"oo"), ','))
  }

  @Test
  def loopbackTest: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))
    val csvStringOrig = file.asCharSource(Charsets.UTF_8).read()

    val readOrigCells = new CsvReader(csvStringOrig).raiseOnError.toIndexedSeq

    assertEquals("Initial data cell count sanity check", 1170, readOrigCells.size)

    csvWriter.write(readOrigCells)

    assertEquals(csvStringOrig, stringBuilder.toString())
  }

}
