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

import java.io.{OutputStreamWriter, StringWriter, Writer}

import fi.pelam.csv.stream.CsvWriter

/**
 * This class writes a [[Table]] as CSV to the given `OutputStream`.
 *
 * The stream is closed at the end. This can be used to write CSV files.
 * The CSV format is taken from [[Table.metadata]].
 *
 * Cells contents are each formatted according to their individual
 * [[fi.pelam.csv.cell.Cell.serializedString serializedString]].
 *
 * {{{
 *  val table = Table[String, String, SimpleMetadata](IndexedSeq(
 *    StringCell(CellKey(0,0), "foo"),
 *    StringCell(CellKey(0,1), "bar")))
 *
 *  val writer = new TableWriter(table)
 *
 *  val outputStream = new ByteArrayOutputStream()
 *
 *  writer.write(outputStream)
 *
 *  val written = new String(outputStream.toByteArray(), table.metadata.charset)
 *
 *  assertEquals("foo,bar\n", written)
 * }}}
 *
 * @param table the table to write
 * @tparam M a user customizable metadata type than can piggybacks additional information on the table object.
 * @tparam RT Client specified object type used for typing rows in CSV data.
 * @tparam CT Client specified object type used for typing columns in CSV data.
 */
final class TableWriter[RT, CT, M <: TableMetadata](table: Table[RT, CT, M]) {

  def write(output: java.io.OutputStream) = {
    try {
      val writer = new OutputStreamWriter(output, table.metadata.charset)

      writeInt(writer)
    } finally {
      output.close()
    }
  }

  /**
   * This is intended for debugging purposes only.
   *
   * Using this method for general output will incur unnecessary conversion
   * to String object. There is also a risk of getting messed up data due to
   * character set problems.
   */
  def writeToString: String = {
    val sw = new StringWriter()
    writeInt(sw)
    sw.toString()
  }

  private def writeInt(writer: Writer): Unit = {
    val csvWriter = new CsvWriter(writer, table.metadata.separator)

    val cells = table.getCells()

    csvWriter.write(cells)

    // Add the final line end
    csvWriter.goToNextRow()

    writer.close()
  }

}
