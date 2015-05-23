package fi.pelam.ahma.serialization

import org.junit.Assert._
import org.junit.Test

import scala.collection.mutable

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
  def testParseQuotes: Unit = {
    val parsed = new CsvReader("\"foo\",\"bar\"\nbaz\n").readAll()
    assertCsv3Cells(parsed)
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

  def assertCsv3Cells(parsed: mutable.Buffer[StringCell]): Unit = {
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseSimpleCells: Unit = {

    val parsed = new CsvReader("Comment,1\nComment,2", ',').readAll()

    val expected = "Cell containing 'Comment' at Row 1, Column A (0)\n" +
      "Cell containing '1' at Row 1, Column B (1)\n" +
      "Cell containing 'Comment' at Row 2, Column A (0)\n" +
      "Cell containing '2' at Row 2, Column B (1)\n"

    assertEquals(expected, parsed.foldLeft("")(_ + _ + "\n"))
  }


}
