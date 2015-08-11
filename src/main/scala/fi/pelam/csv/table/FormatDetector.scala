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

case class LeastErrorsEvaluator[RT, CT, M <: TableMetadata](
  currentResult: Table[RT, CT, M] = Table(),
  currentErrors: TableReadingErrors = TableReadingErrors.initial
  )(readerMaker: LeastErrorsEvaluator.ReaderMaker[RT, CT, M]){

  type ResultTable = Table[RT, CT, M]

  type Reader = TableReader[RT, CT, M]

  def evaluateMetadataCombination(metadataCombination: M): LeastErrorsEvaluator[RT, CT, M] = {

    val reader: Reader = readerMaker(metadataCombination)

    val (result: ResultTable, errors: TableReadingErrors) = reader.read()

    if (errors < currentErrors) {
      copy(currentResult = result, currentErrors = errors)(readerMaker)
    } else {
      this
    }
  }

  def noErrors = currentErrors

}

object LeastErrorsEvaluator {

  type ReaderMaker[RT, CT, M <: TableMetadata] = (M) => TableReader[RT, CT, M]

}

/*
class FormatDetectingReader[RT, CT, M <: TableMetadata](
  val combinationGenerator: FormatDetector.CombinationGenerator,
  val openInputStream: () => java.io.InputStream,
  val rowTyper: TableReader.RowTyper[RT] = PartialFunction.empty,
  val colTyper: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
  val cellUpgrader: TableReader.CellUpgrader[RT, CT] = PartialFunction.empty
  ) {

  import FormatDetector._

  type ResultTable = Table[RT, CT, M]

  val initialEvaluator = new LeastErrorsEvaluator(openInputStream, rowTyper, colTyper, cellUpgrader)

  def read(): (ResultTable, TableReadingErrors) = {

    val combinationGenerator: CombinationGenerator[LeastErrorsEvaluator[RT, CT, M]] = combinationGenerator

    val finalEvaluator = combinationGenerator(initialEvaluator)

    finalEvaluator.result
  }

}

object FormatDetector {
  type CombinationGenerator[E <: FormatEvaluator] = (E) => E

}
*/