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
import org.junit.Test
import org.junit.Assert._
import java.io.ByteArrayInputStream
import java.util.Locale

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell._
import fi.pelam.csv.table.Locales.localeFi
import fi.pelam.csv.table.TestColType._
import fi.pelam.csv.table.TestRowType._
import org.junit.Assert._
import org.junit.Test
import org.mockito.Matchers._
import org.mockito.Mockito.{verify, when, times}
import org.scalatest.mock.MockitoSugar._

class TableReaderEvaluatorTest {
  import TableReaderTest._
  import TableReaderEvaluatorTest._

  val mockReader = mock[TableReader[TestRowType, TestColType, SimpleMetadata]]

  val table1 = Table[TestRowType, TestColType, SimpleMetadata](
    IndexedSeq(StringCell(CellKey(0,0), "foocell")))

  val table2 = Table[TestRowType, TestColType, SimpleMetadata](
    IndexedSeq(StringCell(CellKey(1,0), "barcell")))

  val noErrors = TableReadingErrors()

  val error1 = TableReadingError("foo")

  val error2 = TableReadingError("bar")

  val badErrors = TableReadingErrors(2, IndexedSeq(error1))

  val worseErrorsSameStage = TableReadingErrors(2, IndexedSeq(error1, error2))

  val worseErrorsEarlierStage = TableReadingErrors(1, IndexedSeq(error1))

  @Test
  def testBetterThanInitialResult: Unit = {
    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, SimpleMetadata]()

    when(mockReader.read()).thenReturn((table1, worseErrorsSameStage))

    val finalEvaluator = initialEvaluator.evaluateReader(mockReader)

    assertEquals(Some((table1, worseErrorsSameStage)), finalEvaluator.result)
  }


  @Test
  def testSecondWorseResult: Unit = {
    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, SimpleMetadata]()

    when(mockReader.read()).thenReturn((table1, badErrors))

    val secondEvaluator = initialEvaluator.evaluateReader(mockReader)

    verify(mockReader, times(1)).read()

    // This is not even evaluated
    when(mockReader.read()).thenReturn((table2, worseErrorsSameStage))

    verify(mockReader, times(1)).read()

    val finalEvaluator = secondEvaluator.evaluateReader(mockReader)

    assertEquals(Some((table1, badErrors)), finalEvaluator.result)

  }

  @Test
  def testDetectSeparatorAndCharset {
    val readers = for (
      separator <- List(CsvConstants.defaultSeparatorChar, ';');
      charset <- List(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1);
      cellTypeLocale <- locales;
      dataLocale <- locales) yield {
      val metadata = LocaleMetadata(separator = separator,
        charset = charset,
        dataLocale = dataLocale,
        cellTypeLocale = cellTypeLocale)

      new TableReader(() => new ByteArrayInputStream(semicolonLatin1Csv), metadata,
        rowTyper(metadata.cellTypeLocale), colTyper(metadata.cellTypeLocale),
        cellUpgrader(metadata.dataLocale))
    }

    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, LocaleMetadata]()

    val finalEvaluator = readers.foldLeft(initialEvaluator)(_.evaluateReader(_))

    assertTrue("Should be no errors, but got " + finalEvaluator.errors, finalEvaluator.noErrors)

    val metadata = finalEvaluator.metadata.get

    assertEquals("English (or ROOT) locale should have been detected for numeric data.", Locale.ROOT, metadata.dataLocale)
    assertEquals("Finnish locale for cell type strings should have been detected", localeFi, metadata.cellTypeLocale)
    assertEquals("Non standard separator should have veen detected", ';', metadata.separator)
    assertEquals("Unusual charset should have been detected", StandardCharsets.ISO_8859_1, metadata.charset)
  }

  @Test
  def testDetectLocales {

    val readers = for (cellTypeLocale <- locales;
         dataLocale <- locales) yield {
      val metadata = LocaleMetadata(dataLocale, cellTypeLocale)

      new TableReader(rowAndColTypesFiDataEn, metadata, rowTyper(metadata.cellTypeLocale), colTyper(metadata.cellTypeLocale),
          cellUpgrader(metadata.dataLocale))
    }

    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, LocaleMetadata]()

    val finalEvaluator = readers.foldLeft(initialEvaluator)(_.evaluateReader(_))

    assertTrue("Should be no errors, but got " + finalEvaluator.errors, finalEvaluator.noErrors)

    val metadata = finalEvaluator.metadata.get

    assertEquals("English (or ROOT) locale should have been detected for numeric data.", Locale.ROOT, metadata.dataLocale)
    assertEquals("Finnish locale for cell type strings should have been detected", localeFi, metadata.cellTypeLocale)
    assertEquals("Standard separator expected", CsvConstants.defaultSeparatorChar, metadata.separator)
    assertEquals("Usual charset expected", CsvConstants.defaultCharset, metadata.charset)
  }

}

object TableReaderEvaluatorTest {
  import TableReaderTest._

  val locales = List(localeFi, Locale.ROOT)

  // Create CSV in different format
  val semicolonLatin1Csv = rowAndColTypesFiDataEn
    .replace(',', ';')
    .replaceAll("\"12;000.00\"", "\"12,000.00\"") // hack wrongly replaced separator
    .getBytes(StandardCharsets.ISO_8859_1)
}