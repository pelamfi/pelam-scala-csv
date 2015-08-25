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
 * @constructor
 *
 * @param currentResult
 * @param currentErrors
 * @tparam RT
 * @tparam CT
 * @tparam M
 */
case class TableReaderEvaluator[RT, CT, M <: TableMetadata] private[csv] (
  currentResult: Option[Table[RT, CT, M]],
  currentErrors: TableReadingErrors
  ){

  type ResultTable = Table[RT, CT, M]

  type Reader = TableReader[RT, CT, M]

  def evaluateReader(reader: TableReader[RT, CT, M]): TableReaderEvaluator[RT, CT, M] = {
    if (noErrors) {
      // Don't read table over and over again if we already have a solution without errors.
      this
    } else {
      val (result: ResultTable, errors: TableReadingErrors) = reader.read()

      if (errors > currentErrors) {
        // New better result
        copy(currentResult = Some(result), currentErrors = errors)
      } else {
        this
      }
    }
  }

  def noErrors = currentErrors.noErrors

}

object TableReaderEvaluator {
  def apply[RT, CT, M <: TableMetadata](): TableReaderEvaluator[RT, CT, M] = {
    TableReaderEvaluator(None, TableReadingErrors.initial)
  }
}
