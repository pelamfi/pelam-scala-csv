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

import java.io.OutputStreamWriter

import fi.pelam.csv.stream.CsvWriter

class TableWriter[RT, CT, M <: TableMetadata](table: Table[RT, CT, M]) {

  def write(output: java.io.OutputStream) = {

    val writer = new OutputStreamWriter(output, table.metadata.charset)

    val csvWriter = new CsvWriter(writer, table.metadata.separator)

    val cells = table.getCells()

    csvWriter.write(cells)

    // Add the final line end
    csvWriter.goToNextRow()

    // TODO: Make sure streams are closed if exception is thrown
    writer.close()

    output.close()
  }

}
