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
 * @param readerMaker
 * @tparam RT
 * @tparam CT
 * @tparam M
 */
case class FormatDetectingReader[RT, CT, M <: TableMetadata] private[csv] (
  currentResult: Option[Table[RT, CT, M]],
  currentErrors: TableReadingErrors
  )(readerMaker: FormatDetectingReader.ReaderMaker[RT, CT, M]){

  type ResultTable = Table[RT, CT, M]

  type Reader = TableReader[RT, CT, M]

  def evaluateMetadataCombination(metadataCombination: M): FormatDetectingReader[RT, CT, M] = {
    if (noErrors) {
      // Don't read table over and over again if we already have a solution without errors.
      this
    } else {
      val reader: Reader = readerMaker(metadataCombination)

      val (result: ResultTable, errors: TableReadingErrors) = reader.read()

      if (errors < currentErrors) {
        // New better result
        copy(currentResult = Some(result), currentErrors = errors)(readerMaker)
      } else {
        this
      }
    }
  }

  def noErrors = currentErrors.noErrors

}

object FormatDetectingReader {
  def apply[RT, CT, M <: TableMetadata](readerMaker: FormatDetectingReader.ReaderMaker[RT, CT, M]): FormatDetectingReader[RT, CT, M] = {
    FormatDetectingReader(None, TableReadingErrors.initial)(readerMaker)
  }

  type ReaderMaker[RT, CT, M <: TableMetadata] = (M) => TableReader[RT, CT, M]

}
