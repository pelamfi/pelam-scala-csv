package fi.pelam.csv.table

import java.io.ByteArrayInputStream
import java.util.Locale

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell._
import fi.pelam.csv.table.TestColType._
import fi.pelam.csv.table.TestRowType._
import fi.pelam.csv.util.SortedBiMap
import org.junit.Assert._
import org.junit.Test

class TableReaderTest {
  val headerAndCommentsOnly = "ColumnHeader,CommentCol,CommentCol,CommentCol,CommentCol\n" +
    "CommentRow,1,2,3,4\n" +
    "CommentRow\n" +
    "CommentRow,\n"

  val rowAndColTypesFiDataEn = "KommenttiRivi,1,2,3,4\n" +
    "SarakeOtsikko,Tyypit,TyöntekijäId,IntParam1,Palkka\n" +
    "Työntekijä,ValueCC,4001,8,\"12,000.00\"\n"

  val commentsOnlyFi = "CommentRow,1,2,3,4\nCommentRow\nCommentRow,\n"

  val noRowTypes = "1,2,3,4,\nCommentRow\n\n"

  val rowTyper: TableReader.RowTyper[TestRowType, TestColType] = {
    case (cell, types) if cell.colKey.index == 0 => {
      TestRowType.translations(types.locale).get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown row type."))
      }
    }
  }

  val colTyper: TableReader.ColTyper[TestRowType, TestColType] = {
    case (cell, _) if cell.colKey.index == 0 => Right(TestColType.RowType)
    case (cell, types) if types.rowTypes.get(cell.rowKey) == Some(TestRowType.ColumnHeader) => {
      TestColType.translations(types.locale).get(cell.serializedString) match {
        case Some(x) => Right(x)
        case _ => Left(TableReadingError("Unknown column type."))
      }
    }
  }

  implicit def opener(string: String): () => java.io.InputStream = {
    () => new ByteArrayInputStream(string.getBytes(Charsets.UTF_8))
  }

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }

  @Test(expected = classOf[RuntimeException])
  def testReadFailNoRowId: Unit = {
    // no row types so error
    new TableReader(noRowTypes, rowTyper, colTyper).read()
  }

  @Test
  def testJustReadSimple: Unit = {
    // Works because row type identified
    new TableReader(headerAndCommentsOnly).read()
  }

  @Test
  def testRowCount: Unit = {
    val table = new TableReader(headerAndCommentsOnly).read()
    assertEquals(4, table.rowCount)
  }

  @Test
  def testColCount: Unit = {
    val table = new TableReader(headerAndCommentsOnly).read()
    assertEquals(5, table.colCount)
  }

  @Test
  def testRowTypeFi: Unit = {
    val table = new TableReader(headerAndCommentsOnly, rowTyper, colTyper).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testRowType: Unit = {
    val table = new TableReader(headerAndCommentsOnly, rowTyper, colTyper).read()
    assertEquals(ColumnHeader, table.rowTypes(RowKey(0)))
    assertEquals(CommentRow, table.rowTypes(RowKey(1)))
    assertEquals(CommentRow, table.rowTypes(RowKey(2)))
  }

  @Test
  def testParseLocalizedNumbersAndQuotes: Unit = {
    val input = "Title,Salary,BoolParam1\nWorker,\"12,000.00\",TRUE\n"
    val table = new TableReader(input).read()
    val cells = table.getCells(RowKey(1))
    assertEquals("Worker\n12,000.00\nTRUE\n", cells.foldLeft("")(_ + _.serializedString + "\n"))
  }

  @Test
  def testUpgradeCellType: Unit = {
    val table = new TableReader[TestRowType, TestColType](rowAndColTypesFiDataEn, rowTyper, colTyper,
      Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell), List(Locale.ROOT, Locales.localeFi)).read()

    val cells = table.getSingleCol(TestColType.Salary, TestRowType.Worker)

    assertEquals(Locale.ROOT, table.dataLocale)

    val expectedIntegerCell = IntegerCell.deserialize(CellKey(2, 4), Locale.ROOT, "12000").right.get

    assertEquals(IndexedSeq(expectedIntegerCell), cells)
  }

  @Test
  def testUpgradeCellTypeParsingFailed: Unit = {
    val brokenData = rowAndColTypesFiDataEn.replace("\"12,000.00\"", "injected-error-should-be-number")

    try {
      new TableReader[TestRowType, TestColType](brokenData, rowTyper, colTyper,
        Map(CellType(TestRowType.Worker, TestColType.Salary) -> IntegerCell),
        locales = Seq(Locale.ROOT, Locales.localeFi)).read()
      fail()
    } catch {
      case e: Exception => {
        assertEquals("Failed to parse data in some cells and or identify language/locale.\n" +
          "Error parsing cell content: Expected integer, but input 'injected-error-should-be-number' could not be " +
          "fully parsed with locale 'fi'. CellType(Worker,Salary) Cell containing " +
          "'injected-error-should-be-number' at Row 3, Column E (4)\n", e.getMessage())
      }
    }
  }

  @Test
  def testBuildCellTypesRow: Unit = {
    assertEquals(CellTypes(
      rowTypes = SortedBiMap(RowKey(0) -> TestRowType.CommentRow),
      locale = Locale.ROOT),
      TableReader.buildCellTypes(List(StringCell(CellKey(0, 0), "CommentRow")), Locale.ROOT, rowTyper, PartialFunction.empty))
  }

  @Test
  def testBuildCellTypesCol: Unit = {
    assertEquals(CellTypes(
      rowTypes = SortedBiMap(RowKey(0) -> TestRowType.ColumnHeader),
      colTypes = SortedBiMap(ColKey(0) -> TestColType.RowType),
      locale = Locale.ROOT),
      TableReader.buildCellTypes(List(StringCell(CellKey(0, 0), "ColumnHeader")), Locale.ROOT, rowTyper, colTyper))
  }

  @Test
  def testBuildCellTypesError: Unit = {
    val cell = StringCell(CellKey(0, 0), "Bogus")
    assertEquals(CellTypes(
      errors = IndexedSeq(TableReadingError("Unknown row type.", Some(cell))),
      locale = Locale.ROOT),
      TableReader.buildCellTypes(List(cell), Locale.ROOT, rowTyper, PartialFunction.empty))
  }

  @Test
  def testGetRowAndColTypes: Unit = {
    val table = new TableReader(rowAndColTypesFiDataEn, rowTyper, colTyper, locales = Seq(Locale.ROOT, Locales.localeFi)).read()
    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary), table.colTypes.values.toList)
    assertEquals(List(TestRowType.CommentRow, ColumnHeader, Worker), table.rowTypes.values.toList)
  }

  @Test
  def readCompletefileFiUtf8Csv: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val table = new TableReader(file, rowTyper, colTyper).read()

    assertEquals(List(RowType, Qualifications, WorkerId, IntParam1, Salary,
      BoolParam1, PrevWeek, PrevWeek, PrevWeek), table.colTypes.values.toList.slice(0, 9))

    assertEquals(List(CommentRow, CommentRow, ColumnHeader, Day, Worker, Worker),
      table.rowTypes.values.toList.slice(0, 6))

    assertEquals("Qualifications for all workers should match.", "MSc/MSP,BSc,MBA",
      table.getSingleCol(Qualifications, Worker).map(_.serializedString).reduce(_ + "," + _))

  }

}
