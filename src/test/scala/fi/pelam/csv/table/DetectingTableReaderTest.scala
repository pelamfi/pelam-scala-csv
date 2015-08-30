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

class DetectingTableReaderTest {
  import TableReaderTest._
  import TableReaderEvaluatorTest._

  @Test
  def testDetectLocales {
    val initialMetadata = LocaleMetadata()

    val detectingReader = new DetectingTableReader(initialMetadata = initialMetadata,
      tableReaderMaker = { metadata: LocaleMetadata =>
        new TableReader(rowAndColTypesFiDataEn, metadata, rowTyper(metadata.cellTypeLocale), colTyper(metadata.cellTypeLocale),
          cellUpgrader(metadata.dataLocale))
      },
      locales = locales
    )

    val (table, errors) = detectingReader.read()

    assertTrue("Should be no errors, but got " + errors, errors.noErrors)

    val metadata = table.metadata

    assertEquals("English (or ROOT) locale should have been detected for numeric data.", Locale.ROOT, metadata.dataLocale)
    assertEquals("Finnish locale for cell type strings should have been detected", localeFi, metadata.cellTypeLocale)
    assertEquals("Standard separator expected", CsvConstants.defaultSeparatorChar, metadata.separator)
    assertEquals("Usual charset expected", CsvConstants.defaultCharset, metadata.charset)
  }


}

object DetectingTableReaderTest {
  import TableReaderTest._

  val locales = List(localeFi, Locale.ROOT)

  // Create CSV in different format
  val semicolonLatin1Csv = rowAndColTypesFiDataEn
    .replace(',', ';')
    .replaceAll("\"12;000.00\"", "\"12,000.00\"") // hack wrongly replaced separator
    .getBytes(StandardCharsets.ISO_8859_1)
}