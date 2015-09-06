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

import java.io.ByteArrayInputStream
import java.nio.charset.{Charset, StandardCharsets}
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.table.Locales.localeFi
import org.junit.Assert._
import org.junit.Test

class DetectingTableReaderTest {

  import DetectingTableReaderTest._
  import TableReaderConfig._
  import TableReaderTest._

  @Test
  def testDetectLocales {
    val detectingReader = DetectingTableReader(
      tableReaderMaker = { metadata: LocaleMetadata =>
        new TableReader(rowAndColTypesFiDataEn,
          metadata,
          testRowTyper(metadata.cellTypeLocale),
          testColTyper(metadata.cellTypeLocale),
          cellUpgrader(metadata.dataLocale))
      },
      locales = locales
    )

    val (table, errors) = detectingReader.read()

    assertResultTypeLocaleFiDataEn(table, errors)
  }

  @Test
  def testCustomMetadata {
    val detectingReader = new DetectingTableReader(
      initialMetadata = LocaleMetadataCustom("foo"),
      tableReaderMaker = { metadata: LocaleMetadataCustom =>
        new TableReader(rowAndColTypesFiDataEn,
          // Check that metadata can be replaced here
          metadata.copy(piggybacked = metadata.piggybacked + metadata.cellTypeLocale),
          testRowTyper(metadata.cellTypeLocale),
          testColTyper(metadata.cellTypeLocale),
          cellUpgrader(metadata.dataLocale))
      },
      locales = locales
    )

    val (table, errors) = detectingReader.read()

    assertEquals("Piggybacked data is passed along the chain", "foofi", table.metadata.piggybacked)

    assertResultTypeLocaleFiDataEn(table, errors)
  }

  @Test
  def testDetectSeparatorAndCharset {
    val detectingReader = DetectingTableReader(
      tableReaderMaker = { metadata: LocaleMetadata =>
        new TableReader(() => new ByteArrayInputStream(semicolonLatin1Csv),
          metadata,
          testRowTyper(metadata.cellTypeLocale),
          testColTyper(metadata.cellTypeLocale),
          cellUpgrader(metadata.dataLocale))
      },
      locales = locales
    )

    val (table, errors) = detectingReader.read()

    assertTrue("Should be no errors, but got " + errors, errors.noErrors)

    val metadata = table.metadata

    assertEquals("English (or ROOT) locale should have been detected for numeric data.", Locale.ROOT, metadata.dataLocale)
    assertEquals("Finnish locale for cell type strings should have been detected", localeFi, metadata.cellTypeLocale)
    assertEquals("Non standard separator should have veen detected", ';', metadata.separator)
    assertEquals("Unusual charset should have been detected", StandardCharsets.ISO_8859_1, metadata.charset)
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

  def assertResultTypeLocaleFiDataEn[M <: LocaleTableMetadata[M]](
    table: Table[TestRowType, TestColType, M],
    errors: TableReadingErrors): Unit = {

    assertTrue("Should be no errors, but got " + errors, errors.noErrors)

    val metadata = table.metadata

    assertEquals("English (or ROOT) locale should have been detected for numeric data.", Locale.ROOT, metadata.dataLocale)
    assertEquals("Finnish locale for cell type strings should have been detected", localeFi, metadata.cellTypeLocale)
    assertEquals("Standard separator expected", CsvConstants.defaultSeparatorChar, metadata.separator)
    assertEquals("Usual charset expected", CsvConstants.defaultCharset, metadata.charset)
  }

  case class LocaleMetadataCustom(val piggybacked: String,
    override val dataLocale: Locale = Locale.ROOT,
    override val cellTypeLocale: Locale = Locale.ROOT,
    override val charset: Charset = CsvConstants.defaultCharset,
    override val separator: Char = CsvConstants.defaultSeparatorChar) extends LocaleTableMetadata[LocaleMetadataCustom] {

    override def withFormatParameters(separator: Char,
      charset: Charset,
      cellTypeLocale: Locale,
      dataLocale: Locale): LocaleMetadataCustom = {

      copy(separator = separator, charset = charset, cellTypeLocale = cellTypeLocale, dataLocale = dataLocale)
    }
  }
}