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

class TableReaderEvaluatorTest {
  import TableReaderTest._

  @Test
  def testEvaluateMetadataCombination {
    val locales = List(localeFi, Locale.ROOT)

    val readers = for (dataLocale <- locales;
         cellTypeLocale <- locales) yield {
      val metadata = LocaleTableMetadata(dataLocale, cellTypeLocale)

      new TableReader(rowAndColTypesFiDataEn, metadata, rowTyper(metadata.cellTypeLocale), colTyper(metadata.cellTypeLocale),
          cellUpgrader(metadata.dataLocale))
    }

    val initialEvaluator = TableReaderEvaluator[TestRowType, TestColType, LocaleTableMetadata]()

    val finalEvaluator = readers.foldLeft(initialEvaluator)(_.evaluateReader(_))

    assertTrue("Should be no errors, but got " + finalEvaluator.currentErrors, finalEvaluator.noErrors)

    val table = finalEvaluator.currentResult.get

    assertEquals(Locale.ROOT, table.metadata.dataLocale)
  }

}