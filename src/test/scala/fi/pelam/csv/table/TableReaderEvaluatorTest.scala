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

import fi.pelam.csv.cell._
import org.junit.Assert._
import org.junit.Test
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.mock.MockitoSugar._

class TableReaderEvaluatorTest {

  val mockReader = mock[TableReader[TestRowType, TestColType, SimpleMetadata]]

  val table1 = Table[TestRowType, TestColType, SimpleMetadata](
    IndexedSeq(StringCell(CellKey(0,0), "foocell")))

  val table2 = Table[TestRowType, TestColType, SimpleMetadata](
    IndexedSeq(StringCell(CellKey(1,0), "barcell")))

  val noErrors = TableReadingErrors()

  val error1 = TableReadingError("foo")

  val error2 = TableReadingError("bar")

  val badErrorsStage2 = TableReadingErrors(2, IndexedSeq(error1))

  val worseErrorsStage2 = TableReadingErrors(2, IndexedSeq(error1, error2))

  val worseErrorsStage1 = TableReadingErrors(1, IndexedSeq(error1, error2))

  @Test
  def testBetterThanInitialResult: Unit = {
    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, SimpleMetadata]()

    when(mockReader.read()).thenReturn((table1, worseErrorsStage2))

    val finalEvaluator = initialEvaluator.evaluateReader(mockReader)

    assertEquals(Some((table1, worseErrorsStage2)), finalEvaluator.result)
  }


  @Test
  def testSecondWorseResult: Unit = {
    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, SimpleMetadata]()

    when(mockReader.read()).thenReturn((table1, badErrorsStage2))

    val secondEvaluator = initialEvaluator.evaluateReader(mockReader)

    verify(mockReader, times(1)).read()

    // This is evaluated, but result is ignored due to less errors from earlier
    when(mockReader.read()).thenReturn((table2, worseErrorsStage2))

    val finalEvaluator = secondEvaluator.evaluateReader(mockReader)

    verify(mockReader, times(2)).read()

    assertEquals(Some((table1, badErrorsStage2)), finalEvaluator.result)
  }

  @Test
  def testSecondResultDifferentStage: Unit = {
    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, SimpleMetadata]()

    when(mockReader.read()).thenReturn((table1, worseErrorsStage1))

    val secondEvaluator = initialEvaluator.evaluateReader(mockReader)

    verify(mockReader, times(1)).read()

    // This is taken, because later stage is better
    when(mockReader.read()).thenReturn((table2, worseErrorsStage2))

    val finalEvaluator = secondEvaluator.evaluateReader(mockReader)

    verify(mockReader, times(2)).read()

    assertEquals(Some((table2, worseErrorsStage2)), finalEvaluator.result)

  }

  @Test
  def testNoErrors: Unit = {
    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, SimpleMetadata]()

    when(mockReader.read()).thenReturn((table1, noErrors))

    val secondEvaluator = initialEvaluator.evaluateReader(mockReader)

    verify(mockReader, times(1)).read()

    when(mockReader.read()).thenReturn((table2, worseErrorsStage2))

    val finalEvaluator = secondEvaluator.evaluateReader(mockReader)

    // Read was not called, because already error free result
    verify(mockReader, times(1)).read()

    assertEquals(Some((table1, noErrors)), finalEvaluator.result)

  }


}

