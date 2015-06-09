package fi.pelam.ahma.serialization

import com.google.common.base.Charsets
import com.google.common.io.Resources
import org.junit.Assert._
import org.junit.Test

class CsvReaderTest {

  val csv3Cells: String = "foo,bar\nbaz\n"

  @Test
  def testParse3: Unit = {
    val parsed = new CsvReader(csv3Cells).readAll()
    assertEquals(3, parsed.size)
  }

  @Test
  def testParseContent: Unit = {
    val parsed = new CsvReader(csv3Cells).readAll()
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseIndices: Unit = {
    val parsed = new CsvReader(csv3Cells).readAll()
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseCrLf: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz\n").readAll()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseUnterminatedLastLine: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz").readAll()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseUnterminatedLastLineWithExtraCells: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz,x,y").readAll()

    assertEquals("foo\nbar\nbaz\nx\ny\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n(1,1)\n(1,2)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))

  }

  @Test
  def testParseUnterminatedLastLineWithEmptyCells: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz,,").readAll()

    assertEquals("foo\nbar\nbaz\n\n\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n(1,1)\n(1,2)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))

  }

  @Test
  def testParseZeroLengthInput: Unit = {
    val reader = new CsvReader("")
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseInputWithOnlyLinefeed: Unit = {
    val reader = new CsvReader("\n")
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), ""), reader.next())
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseInputWithOnlyLinefeedCrLf: Unit = {
    val reader = new CsvReader("\r\n")
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), ""), reader.next())
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseUnterminatedLine: Unit = {
    val reader = new CsvReader("x,y")
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), "x"), reader.next())
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 1), "y"), reader.next())
    assertFalse(reader.hasNext)
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseEmptyCellsAndUnterminatedLine: Unit = {
    val reader = new CsvReader(",,")
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), ""), reader.next())
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 1), ""), reader.next())
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 2), ""), reader.next())
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseQuotes: Unit = {
    val parsed = new CsvReader("\"foo\",\"bar\"\nbaz\n").toIndexedSeq
    assertCsv3Cells(parsed)
  }

  @Test(expected = classOf[RuntimeException])
  def testBrokenQuotes: Unit = {
    new CsvReader("\"foo\n")
  }

  @Test(expected = classOf[RuntimeException])
  def testBrokenQuotes2: Unit = {
    new CsvReader("\"foo")
  }

  @Test
  def testParseComplexQuotes: Unit = {
    val parsed = new CsvReader("fo\"o\",\"b\"ar\nbaz\n").readAll()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseQuoteCharsWithinQuotes: Unit = {
    val parsed = new CsvReader("\"f\"\"oo\",bar\nbaz\n").readAll()
    assertEquals("f\"oo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseChangeSeparator: Unit = {
    val parsed = new CsvReader("foo;bar\nbaz\n", separator = ';').readAll()
    assertCsv3Cells(parsed)
  }

  def assertCsv3Cells(parsed: Seq[StringCell]): Unit = {
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseSimpleCells: Unit = {

    val parsed = new CsvReader("Comment,1\nComment,2\n", ',').readAll()

    val expected = "Cell containing 'Comment' at Row 1, Column A (0)\n" +
      "Cell containing '1' at Row 1, Column B (1)\n" +
      "Cell containing 'Comment' at Row 2, Column A (0)\n" +
      "Cell containing '2' at Row 2, Column B (1)\n"

    assertEquals(expected, parsed.foldLeft("")(_ + _ + "\n"))
  }

  @Test
  def testLargeFile: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val csvString = file.asCharSource(Charsets.UTF_8).read()

    val cells = new CsvReader(csvString).readAll()

    assertEquals("Expected number of cells", 1170, cells.size)
  }

}
