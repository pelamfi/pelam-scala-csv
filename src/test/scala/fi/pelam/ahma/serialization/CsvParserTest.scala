package fi.pelam.ahma.serialization

import org.junit.Assert._
import org.junit.Test

class CsvParserTest {

  @Test
  def testParse: Unit = {
    val parsed = new CsvParser("foo,bar\nbaz\n").parse()
    assertEquals(3, parsed.size)
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseCrLf: Unit = {
    val parsed = new CsvParser("foo,bar\r\nbaz\n").parse()
    assertEquals(3, parsed.size)
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseQuotes: Unit = {
    val parsed = new CsvParser("\"foo\",\"bar\"\nbaz\n").parse()
    assertEquals(3, parsed.size)
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseComplexQuotes: Unit = {
    val parsed = new CsvParser("fo\"o\",\"b\"ar\nbaz\n").parse()
    assertEquals(3, parsed.size)
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

}
