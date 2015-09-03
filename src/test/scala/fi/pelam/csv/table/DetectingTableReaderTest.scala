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

import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell._
import fi.pelam.csv.table.Locales.localeFi
import fi.pelam.csv.table.TestColType._
import fi.pelam.csv.table.TestRowType._
import org.junit.Assert._
import org.junit.Test
import fi.pelam.csv.cell._

class DetectingTableReaderTest {

  import TableReaderTest._
  import DetectingTableReaderTest._
  import fi.pelam.csv.util.TableReaderImplicits._

  @Test
  def testDetectLocales {
    val detectingReader = DetectingTableReader(
      tableReaderMaker = { metadata: LocaleMetadata =>
        new TableReader(rowAndColTypesFiDataEn,
          metadata,
          rowTyper(metadata.cellTypeLocale),
          colTyper(metadata.cellTypeLocale),
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
          rowTyper(metadata.cellTypeLocale),
          colTyper(metadata.cellTypeLocale),
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
          rowTyper(metadata.cellTypeLocale),
          colTyper(metadata.cellTypeLocale),
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

  @Test
  def testFromCodeExample() = {
    // TODO: Add this as a code sample

    val reader = DetectingTableReader(

      tableReaderMaker = { (m: LocaleMetadata) => new TableReader[String, String, LocaleMetadata](
          openStream = () => new ByteArrayInputStream(("header;name;number\n" +
          "data;foo;1,234.0\n" +
          "data;bar;1,234,567.89").getBytes(StandardCharsets.ISO_8859_1)),

        tableMetadata = m,

        rowTyper = {
          case StringCell(CellKey(_, 0), rowType) => Right(rowType)
        },

        // Column type is specified by the first row.
        // Type names are checked and error is generated for unknown
        // columns. This strictness is what enables the correct
        // detection of CSV format.
        colTyper = {
          case (StringCell(CellKey(0, _), "header"), _) => Right("header")
          case (StringCell(CellKey(0, _), "name"), _) => Right("name")
          case (StringCell(CellKey(0, _), "number"), _) => Right("number")
          case _ => Left(TableReadingError("Unknown column type"))
        },

        cellUpgrader = TableReader.defineCellUpgrader(m.dataLocale, {
          case CellType("data", "number") => DoubleCell
        }))
      }
    )

    val table = reader.readOrThrow()

    assertEquals(List("foo", "bar"), table.getSingleCol("data", "name").map(_.value).toList)
    assertEquals(List(1234, 1234567.89), table.getSingleCol("data", "number").map(_.value).toList)

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