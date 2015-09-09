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

package fi.pelam.csv.table

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell.{CellKey, StringCell}
import org.junit.Assert._
import org.junit.Test

class TableWriterTest {
  import TableTest._
  import TableReaderTest._
  import TableWriterTest._

  val outputStream = new ByteArrayOutputStream()

  @Test
  def testWrite: Unit = {
    val table = TableTest.table.updatedCells(foo, bar)

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.metadata.charset)

    assertEquals(",,,,,\n,foo,,,,\n,bar,,,,\n,,,,,\n,,,,,\n", written)
  }

  @Test
  def testWriteExample: Unit = {
    val table = Table[String, String, SimpleMetadata](IndexedSeq(
      StringCell(CellKey(0,0), "foo"),
      StringCell(CellKey(0,1), "bar")))

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.metadata.charset)

    assertEquals("foo,bar\n", written)
  }

  @Test
  def testLoopback: Unit = {
    val (table, errors) = new TableReader[TestRowType, TestColType, SimpleMetadata](testFile).read()

    assertTrue(errors.toString, errors.noErrors)

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.metadata.charset)

    assertEquals(testFileContent, written)
  }
}

object TableWriterTest {

  val testFileName = "csv–file-for-loading"

  lazy val testFile = Resources.asByteSource(Resources.getResource(testFileName))

  val testFileCharset = StandardCharsets.UTF_8

  // Added \n because table writer ensures there is traling newline
  lazy val testFileContent = readTestFile(testFileName) + "\n"

  def readTestFile(resource: String) = {
    val testFile = Resources.asByteSource(Resources.getResource(resource))
    new String(testFile.read(), testFileCharset)
  }

}
