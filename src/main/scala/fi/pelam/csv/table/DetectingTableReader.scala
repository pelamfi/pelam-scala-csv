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

import java.nio.charset.{StandardCharsets, Charset}
import fi.pelam.csv.CsvConstants
import java.io.ByteArrayInputStream
import java.util.Locale
import fi.pelam.csv.cell._

/**
  * This CSV format detection heuristic tries to read the input CSV
  * with varying parameters until a `Table` is produced with no errors or
  * all combinations are exhausted. In the latter case
  * the `Table` that was produced with least errors is returned.
  *
  * @tparam RT The client specific row type.
  *
  * @tparam CT The client specific column type.
  *
  * @tparam M The type of the `metadata` parameter. Must be a sub type of [[TableMetadata]].
  *           This specifies the character set and separator to use when reading the CSV data from the input stream.
 */
class DetectingTableReader[RT, CT, M <: LocaleTableMetadata](
  val initialMetadata: M,
  val tableReaderMaker: (M) => TableReader[RT, CT, M],
  val locales: Seq[Locale] = CsvConstants.commonLocales,
  val charsets: Seq[Charset] = CsvConstants.commonCharsets,
  val separators: Seq[Char] = CsvConstants.commonSeparators
  ) {

  type ResultTable = Table[RT, CT, M]

  def read(): (ResultTable, TableReadingErrors) = {

    tableReaderMaker(initialMetadata).read()

  }

}
