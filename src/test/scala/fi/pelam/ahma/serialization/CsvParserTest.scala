package fi.pelam.ahma.serialization

import org.junit.Assert._
import org.junit.Test

import scala.collection.mutable

class CsvParserTest {

  val csv3Cells: String = "foo,bar\nbaz\n"

  @Test
  def testParse3: Unit = {
    val parsed = new CsvParser(csv3Cells).parse()
    assertEquals(3, parsed.size)
  }

  @Test
  def testParseContent: Unit = {
    val parsed = new CsvParser(csv3Cells).parse()
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseIndices: Unit = {
    val parsed = new CsvParser(csv3Cells).parse()
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseCrLf: Unit = {
    val parsed = new CsvParser("foo,bar\r\nbaz\n").parse()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseUnterminatedLastLine: Unit = {
    val parsed = new CsvParser("foo,bar\r\nbaz").parse()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseQuotes: Unit = {
    val parsed = new CsvParser("\"foo\",\"bar\"\nbaz\n").parse()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseComplexQuotes: Unit = {
    val parsed = new CsvParser("fo\"o\",\"b\"ar\nbaz\n").parse()
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseQuoteCharsWithinQuotes: Unit = {
    val parsed = new CsvParser("\"f\"\"oo\",bar\nbaz\n").parse()
    assertEquals("f\"oo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseChangeSeparator: Unit = {
    val parsed = new CsvParser("foo;bar\nbaz\n", separator = ';').parse()
    assertCsv3Cells(parsed)
  }

  def assertCsv3Cells(parsed: mutable.Buffer[StringCell]): Unit = {
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }


}
