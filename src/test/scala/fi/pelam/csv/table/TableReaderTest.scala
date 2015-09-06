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

import java.util.Locale

import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell._
import fi.pelam.csv.table.Locales.localeFi
import fi.pelam.csv.table.TestColType._
import fi.pelam.csv.table.TestRowType._
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {
  import TableReaderConfig._
  import TableReaderTest._

  @Test
  def testReadFailNoRowId: Unit = {
    // no row types so error
    val (table, errors) = new TableReader(noRowTypes, SimpleMetadata(), testRowTyper(Locale.ROOT), testColTyper(Locale.ROOT)).read()

    assertFalse(errors.noErrors)

    assertEquals(
      "Unknown row type. The error is related to the StringCell with value '1' at Row 1, Column A (0).\n" +
        "Unknown row type. The error is related to the StringCell with value '' at Row 3, Column A (0).\n",
      errors.errors.foldLeft("")(_ + _.toString() + "\n"))
  }

  @Test
  def testJustReadSimple: Unit = {
    // Works because row type identified
    val (table, errors) = new TableReader(headerAndCommentsOnly).read()
    assertTrue(errors.noErrors)
  }

  @Test
  def testRowCount: Unit = {
    val (table, errors) = new TableReader(headerAndCommentsOnly).read()
    assertEquals(4, table.rowCount)
  }

  @Test
  def testColCount: Unit = {
    val (table, errors) = new TableReader(headerAndCommentsOnly).read()
    assertEquals(5, table.colCount)
  }

  @Test
  def testRowTypeEn: Unit = {
    val metadata = SimpleMetadata()
    val (table, errors) = new TableReader(headerAndCommentsOnly, metadata, testRowTyper(Locale.ROOT), testColTyper(Locale.ROOT)).read()
    assertTrue(errors.toString, errors.noErrors)
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testRowTypeFi: Unit = {
    val (table, errors) = new TableReader(headerAndCommentsOnlyFi, SimpleMetadata(), testRowTyper(localeFi), testColTyper(localeFi)).read()
    assertTrue(errors.toString, errors.noErrors)
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testParseLocalizedNumbersAndQuotes: Unit = {
    val input = "Title,Salary,BoolParam1\nWorker,\"12,000.00\",TRUE\n"
    val (table, errors) = new TableReader(input).read()
    assertTrue(errors.toString, errors.noErrors)
    val cells = table.getCells(RowKey(1))
    assertEquals("Worker\n12,000.00\nTRUE\n", cells.foldLeft("")(_ + _.value + "\n"))
  }

  @Test
  def testUpgradeCellType: Unit = {

    val (table, errors) = new TableReader(rowAndColTypesFiDataEn, SimpleMetadata(), testRowTyper(localeFi), testColTyper(localeFi),
      cellUpgrader(Locale.ROOT)).read()

    assertTrue(errors.toString, errors.noErrors)

    val cells = table.getSingleCol(TestRowType.Worker, TestColType.Salary)

    // TODO: wrap in locale detection
    // assertEquals(Locale.ROOT, table.metadata.dataLocale)

    val expectedIntegerCell = IntegerCell.parse(CellKey(2, 4), Locale.ROOT, "12000").right.get

    assertEquals(IndexedSeq(expectedIntegerCell), cells)
  }

  @Test
  def testUpgradeCellTypeParsingFailed: Unit = {
    val brokenData = rowAndColTypesFiDataEn.replace("\"12,000.00\"", "injected-error-should-be-number")

    val (table, errors) = new TableReader(brokenData, SimpleMetadata(), testRowTyper(localeFi), testColTyper(localeFi),
      cellUpgrader(localeFi)).read()

    assertEquals(
      "Error parsing cell content: Expected integer, but input 'injected-error-should-be-number' could not be " +
        "fully parsed with locale 'fi'. CellType(Worker,Salary) The error is related to the StringCell with value " +
        "'injected-error-should-be-number' at Row 3, Column E (4).", errors.errors(0).toString())

    assertEquals(1, errors.errors.size)
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    // TODO: test with locale detection heuristic
    val (table, errors) = new TableReader(rowAndColTypesFiDataEn, SimpleMetadata(), testRowTyper(localeFi), testColTyper(localeFi),
      cellUpgrader(Locale.ROOT)).read()

    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary), table.colTypes.values.toList)
    assertEquals(List(TestRowType.CommentRow, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val (table, errors) = new TableReader(file, SimpleMetadata(), testRowTyper(Locale.ROOT), testColTyper(Locale.ROOT),
      cellUpgrader(Locale.ROOT)).read()

    assertTrue(errors.toString, errors.noErrors)

    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary,
      BoolParam1, PrevWeek, PrevWeek, PrevWeek), table.colTypes.values.toList.slice(0, 9))

    assertEquals(List(CommentRow, CommentRow, ColumnHeader, Day, Worker, Worker),
      table.rowTypes.values.toList.slice(0, 6))

    assertEquals("Qualifications for all workers should match.", "MSc/MSP,BSc,MBA",
      table.getSingleCol(Worker, Qualifications).map(_.value).reduce(_ + "," + _))

  }
}

object TableReaderTest {
  import TableReaderConfig._

  val headerAndCommentsOnly = "ColumnHeader,CommentCol,CommentCol,CommentCol,CommentCol\n" +
    "CommentRow,1,2,3,4\n" +
    "CommentRow\n" +
    "CommentRow,\n"

  val headerAndCommentsOnlyFi = "SarakeOtsikko,KommenttiSarake,KommenttiSarake,KommenttiSarake,KommenttiSarake\n" +
    "KommenttiRivi,1,2,3,4\n" +
    "KommenttiRivi\n" +
    "KommenttiRivi,\n"

  val rowAndColTypesFiDataEn = "KommenttiRivi,1,2,3,4\n" +
    "SarakeOtsikko,Tyypit,TyöntekijäId,IntParam1,Palkka\n" +
    "Työntekijä,ValueCC,4001,8,\"12,000.00\"\n"

  val commentsOnlyFi = "CommentRow,1,2,3,4\nCommentRow\nCommentRow,\n"

  val noRowTypes = "1,2,3,4,\nCommentRow\n\n"

  def testRowTyper(cellTypeLocale: Locale): TableReader.RowTyper[TestRowType] = {
    case (cell) if cell.colKey.index == 0 => {
      TestRowType.translations(cellTypeLocale).get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown row type."))
      }
    }
  }

  def testColTyper(cellTypeLocale: Locale): TableReader.ColTyper[TestRowType, TestColType] = {
    case (cell, _) if cell.colKey.index == 0 => Right(TestColType.RowType)
    case (cell, rowTypes) if rowTypes.get(cell.rowKey) == Some(TestRowType.ColumnHeader) => {
      TestColType.translations(cellTypeLocale).get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown column type."))
      }
    }
  }

  def cellUpgrader(locale: Locale) = makeCellUpgrader[TestRowType, TestColType](
    Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell), locale
  )

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }
}