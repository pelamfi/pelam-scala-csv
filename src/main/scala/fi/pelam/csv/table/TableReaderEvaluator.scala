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

/**
 * This class models a process where several differently constructed [[TableReader]] instances
 * are tried and the result from the one with least, preferably zero, errors is picked.
 *
 * @param table initialized by default to `None`
 * @param errors initialized by default into value that is "worse" than any actual error produced by [[TableReader]].

 * @tparam RT The client specific row type.
 *
 * @tparam CT The client specific column type.
 *
 * @tparam M The type of the `metadata` parameter. Must be a sub type of [[TableMetadata]].
 *           This specifies the character set and separator to use when reading the CSV data from the input stream.
 *
 */
final case class TableReaderEvaluator[RT, CT, M <: TableMetadata] private[csv](
  table: Option[Table[RT, CT, M]] = None,
  errors: TableReadingErrors = TableReadingErrors.initialValue
  ) {

  val metadata: Option[M] = table.map(_.metadata)

  type ResultTable = Table[RT, CT, M]

  type Reader = TableReader[RT, CT, M]

  /**
   * With this method candidate `TableReader` instances are evaluated into updated copy of this instance.
   *
   * If zero errors solution has already been reached, the nothing is done.
   */
  def evaluateReader(reader: Reader): TableReaderEvaluator[RT, CT, M] = {
    if (noErrors) {
      // Don't read table over and over again if we already have a solution without errors.
      this
    } else {
      val (newTable: ResultTable, newErrors: TableReadingErrors) = reader.read()

      if (errors < newErrors) {
        // New better result
        copy(table = Some(newTable), errors = newErrors)
      } else {
        this
      }
    }
  }

  def noErrors = errors.noErrors

  def result: Option[(ResultTable, TableReadingErrors)] = table.map((_, errors))

}
