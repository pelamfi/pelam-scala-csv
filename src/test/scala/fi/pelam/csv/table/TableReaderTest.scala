package fi.pelam.csv.table

import java.io.ByteArrayInputStream
import java.util.Locale

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell._
import fi.pelam.csv.table.Locales.localeFi
import fi.pelam.csv.table.TestColType._
import fi.pelam.csv.table.TestRowType._
import fi.pelam.csv.util.SortedBiMap
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {
  import TableReaderTest._

  @Test
  def testReadFailNoRowId: Unit = {
    // no row types so error
    val (table, errors) = new TableReader2(noRowTypes, SimpleTableMetadata(), rowTyper(Locale.ROOT), colTyper(Locale.ROOT)).read()

    assertFalse(errors.noErrors)

    assertEquals(
      "Unknown row type. Cell containing '1' at Row 1, Column A (0)\n" +
        "Unknown row type. Cell containing '' at Row 3, Column A (0)\n",
      errors.errors.foldLeft("")(_ + _.toString() + "\n"))
  }

  @Test
  def testJustReadSimple: Unit = {
    // Works because row type identified
    val (table, errors) = new TableReader2(headerAndCommentsOnly).read()
    assertTrue(errors.noErrors)
  }

  @Test
  def testRowCount: Unit = {
    val (table, errors) = new TableReader2(headerAndCommentsOnly).read()
    assertEquals(4, table.rowCount)
  }

  @Test
  def testColCount: Unit = {
    val (table, errors) = new TableReader2(headerAndCommentsOnly).read()
    assertEquals(5, table.colCount)
  }

  @Test
  def testRowTypeEn: Unit = {
    val metadata = SimpleTableMetadata()
    val (table, errors) = new TableReader2(headerAndCommentsOnly, metadata, rowTyper(Locale.ROOT), colTyper(Locale.ROOT)).read()
    assertTrue(errors.toString, errors.noErrors)
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testRowTypeFi: Unit = {
    val (table, errors) = new TableReader2(headerAndCommentsOnlyFi, SimpleTableMetadata(), rowTyper(localeFi), colTyper(localeFi)).read()
    assertTrue(errors.toString, errors.noErrors)
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testParseLocalizedNumbersAndQuotes: Unit = {
    val input = "Title,Salary,BoolParam1\nWorker,\"12,000.00\",TRUE\n"
    val (table, errors) = new TableReader2(input).read()
    assertTrue(errors.toString, errors.noErrors)
    val cells = table.getCells(RowKey(1))
    assertEquals("Worker\n12,000.00\nTRUE\n", cells.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testUpgradeCellType: Unit = {

    val (table, errors) = new TableReader2(rowAndColTypesFiDataEn, SimpleTableMetadata(), rowTyper(localeFi), colTyper(localeFi),
      cellUpgrader(Locale.ROOT)).read()

    assertTrue(errors.toString, errors.noErrors)

    val cells = table.getSingleCol(TestColType.Salary, TestRowType.Worker)

    // TODO: wrap in locale detection
    // assertEquals(Locale.ROOT, table.metadata.dataLocale)

    val expectedIntegerCell = IntegerCell.parse(CellKey(2, 4), Locale.ROOT, "12000").right.get

    assertEquals(IndexedSeq(expectedIntegerCell), cells)
  }

  @Test
  def testUpgradeCellTypeParsingFailed: Unit = {
    val brokenData = rowAndColTypesFiDataEn.replace("\"12,000.00\"", "injected-error-should-be-number")

    val (table, errors) = new TableReader2(brokenData, SimpleTableMetadata(), rowTyper(localeFi), colTyper(localeFi),
      cellUpgrader(localeFi)).read()

    assertEquals(
      "Error parsing cell content: Expected integer, but input 'injected-error-should-be-number' could not be " +
      "fully parsed with locale 'fi'. CellType(Worker,Salary) Cell containing " +
      "'injected-error-should-be-number' at Row 3, Column E (4)", errors.errors(0).toString())

    assertEquals(1, errors.errors.size)
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    // TODO: test with locale detection heuristic
    val (table, errors) = new TableReader2(rowAndColTypesFiDataEn, SimpleTableMetadata(), rowTyper(localeFi), colTyper(localeFi),
      cellUpgrader(Locale.ROOT)).read()

    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary), table.colTypes.values.toList)
    assertEquals(List(TestRowType.CommentRow, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val (table, errors) = new TableReader2(file, SimpleTableMetadata(), rowTyper(Locale.ROOT), colTyper(Locale.ROOT),
      cellUpgrader(Locale.ROOT)).read()

    assertTrue(errors.toString, errors.noErrors)

    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary,
      BoolParam1, PrevWeek, PrevWeek, PrevWeek), table.colTypes.values.toList.slice(0, 9))

    assertEquals(List(CommentRow, CommentRow, ColumnHeader, Day, Worker, Worker),
      table.rowTypes.values.toList.slice(0, 6))

    assertEquals("Qualifications for all workers should match.", "MSc/MSP,BSc,MBA",
      table.getSingleCol(Qualifications, Worker).map(_.serializedString).reduce(_ + "," + _))

  }
}

object TableReaderTest {
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

  def rowTyper(cellTypeLocale: Locale): TableReader2.RowTyper[TestRowType] = {
    case (cell) if cell.colKey.index == 0 => {
      TestRowType.translations(cellTypeLocale).get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown row type."))
      }
    }
  }

  def colTyper(cellTypeLocale: Locale): TableReader2.ColTyper[TestRowType, TestColType] = {
    case (cell, _) if cell.colKey.index == 0 => Right(TestColType.RowType)
    case (cell, rowTypes) if rowTypes.get(cell.rowKey) == Some(TestRowType.ColumnHeader) => {
      TestColType.translations(cellTypeLocale).get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown column type."))
      }
    }
  }

  def cellUpgrader(locale: Locale) = TableReader2.defineCellUpgrader[TestRowType, TestColType](
    locale,
    Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell)
  )

  implicit def opener(string: String): () => java.io.InputStream = {
    () => new ByteArrayInputStream(string.getBytes(Charsets.UTF_8))
  }

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }
}