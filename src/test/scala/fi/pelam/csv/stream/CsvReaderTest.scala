/*
 * This file is part of pelam-scala-csv
 *
 * Copyright © Peter Lamberg 2015 (pelam-scala-csv@pelam.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.pelam.csv.stream

import com.google.common.base.Charsets
import com.google.common.io.Resources
import fi.pelam.csv.cell.{CellKey, StringCell}
import org.junit.Assert._
import org.junit.Test

class CsvReaderTest {

  val csv3Cells: String = "foo,bar\nbaz\n"

  @Test
  def testParse3: Unit = {
    val parsed = new CsvReader(csv3Cells).toIndexedSeq
    assertEquals(3, parsed.size)
  }

  @Test
  def testParseContent: Unit = {
    val parsed = new CsvReader(csv3Cells).throwOnError.toIndexedSeq
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseIndices: Unit = {
    val parsed = new CsvReader(csv3Cells).throwOnError.toIndexedSeq
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  def testIteratorEnd: Unit = {
    val reader = new CsvReader(csv3Cells)

    reader.next()
    reader.next()
    reader.next()

    assertFalse(reader.hasNext)
  }

  @Test(expected = classOf[NoSuchElementException])
  def testNoSuchElementException: Unit = {
    val reader = new CsvReader(csv3Cells)

    reader.next()
    reader.next()
    reader.next()

    // This should throw
    reader.next()
  }

  @Test
  def testParseCrLf: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz\n").throwOnError.toIndexedSeq
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseUnterminatedLastLine: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz").throwOnError.toIndexedSeq
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseUnterminatedLastLineWithExtraCells: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz,x,y").throwOnError.toIndexedSeq

    assertEquals("foo\nbar\nbaz\nx\ny\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n(1,1)\n(1,2)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))

  }

  @Test
  def testParseUnterminatedLastLineWithEmptyCells: Unit = {
    val parsed = new CsvReader("foo,bar\r\nbaz,,").throwOnError.toIndexedSeq

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
    val reader = new CsvReader("\n").throwOnError
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), ""), reader.next())
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseInputWithOnlyLinefeedCrLf: Unit = {
    val reader = new CsvReader("\r\n").throwOnError
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), ""), reader.next())
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseUnterminatedLine: Unit = {
    val reader = new CsvReader("x,y").throwOnError
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 0), "x"), reader.next())
    assertTrue(reader.hasNext)
    assertEquals(StringCell(CellKey(0, 1), "y"), reader.next())
    assertFalse(reader.hasNext)
    assertFalse(reader.hasNext)
  }

  @Test
  def testParseEmptyCellsAndUnterminatedLine: Unit = {
    val reader = new CsvReader(",,").throwOnError
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
    val parsed = new CsvReader("\"foo\",\"bar\"\nbaz\n").throwOnError.toIndexedSeq
    assertCsv3Cells(parsed)
  }

  @Test(expected = classOf[RuntimeException])
  def testBrokenQuotes: Unit = {
    new CsvReader("\"foo\n").throwOnError.toIndexedSeq
  }

  @Test(expected = classOf[RuntimeException])
  def testBrokenQuotes2: Unit = {
    try {
      new CsvReader("\"foo").throwOnError.toIndexedSeq
    } catch {
      case e: RuntimeException => {
        assertEquals("java.lang.RuntimeException: Error parsing CSV at Row 1, " +
          "Column A (0): Input stream ended while processing quoted characters.", e.toString)
        throw e
      }
    }
  }

  @Test
  def testErrorState: Unit = {
    val reader = new CsvReader("\"foo")

    assertTrue(reader.hasNext)

    assertEquals(Left(CsvReaderError("Input stream ended while processing quoted characters.", CellKey(0, 0))),
      reader.next())

    assertFalse(reader.hasNext)
  }

  @Test(expected = classOf[NoSuchElementException])
  def testNoSuchElementExceptionAfterError: Unit = {
    val reader = new CsvReader("\"foo")

    reader.next()

    reader.next() // This should throw
  }

  @Test
  def testParseComplexQuotes: Unit = {
    val parsed = new CsvReader("fo\"o\",\"b\"ar\nbaz\n").throwOnError.toIndexedSeq
    assertCsv3Cells(parsed)
  }

  @Test
  def testParseQuoteCharsWithinQuotes: Unit = {
    val parsed = new CsvReader("\"f\"\"oo\",bar\nbaz\n").throwOnError.toIndexedSeq
    assertEquals("f\"oo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testParseChangeSeparator: Unit = {
    val parsed = new CsvReader("foo;bar\nbaz\n", separator = ';').throwOnError.toIndexedSeq
    assertCsv3Cells(parsed)
  }

  def assertCsv3Cells(parsed: Seq[StringCell]): Unit = {
    assertEquals("foo\nbar\nbaz\n", parsed.foldLeft("")(_ + _.serializedString + "\n"))
    assertEquals("(0,0)\n(0,1)\n(1,0)\n", parsed.foldLeft("")(_ + _.cellKey.indices + "\n"))
  }

  @Test
  def testParseSimpleCells: Unit = {

    val parsed = new CsvReader("Comment,1\nComment,2\n", ',').throwOnError.toIndexedSeq

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

    val cells = new CsvReader(csvString).toIndexedSeq

    assertEquals("Expected number of cells", 180, cells.size)
  }

}
