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
import com.google.common.io.{CharStreams, Resources}
import fi.pelam.csv.cell.{CellKey, StringCell}
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

    val readOrigCells = new CsvReader(csvStringOrig).throwOnError.toIndexedSeq

    assertEquals("Initial data cell count sanity check", 180, readOrigCells.size)

    csvWriter.write(readOrigCells)

    assertEquals(csvStringOrig, stringBuilder.toString())
  }

}
