package fi.pelam.csv

import java.util.Locale

import com.google.common.base.Charsets
import org.junit.Assert._
import org.junit.Test

import scala.collection.immutable.TreeMap

object TableTest {
  def makeTable() = Table[TestRowType, TestColType](Charsets.UTF_8,
    CsvConstants.defaultSeparatorChar,
    Locale.ROOT,
  CellTypes[TestRowType, TestColType](
    BiMap(TreeMap(RowKey(0) -> TestRowType.CommentRow,
      RowKey(1) -> TestRowType.Worker,
      RowKey(2) -> TestRowType.Worker,
      RowKey(3) -> TestRowType.Day,
      RowKey(4) -> TestRowType.CommentRow)),

    BiMap(TreeMap(ColKey(1) -> TestColType.Qualifications,
      ColKey(2) -> TestColType.PrevWeek,
      ColKey(3) -> TestColType.PrevWeek,
      ColKey(4) -> TestColType.ThisWeek,
      ColKey(5) -> TestColType.CommentCol)),
    locale = Locale.ROOT), List[Cell]())

  val foo = StringCell(CellKey(1, 1), "foo")
  val bar = StringCell(CellKey(2, 1), "bar")
  val history1 = StringCell(CellKey(3, 2), "history1")
  val history2 = StringCell(CellKey(3, 3), "history2")
  val plan1 = StringCell(CellKey(3, 4), "plan1")
}

class TableTest {

  import TableTest._

  var table = makeTable()

  @Test
  def testGetSingleCol: Unit = {

    table = table.updatedCells(
      StringCell(CellKey(1, 2), "x"),
      foo,
      bar,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, bar), table.getSingleCol(TestColType.Qualifications, TestRowType.Worker).toList)
  }

  @Test
  def testGetSingleRow: Unit = {

    table = table.updatedCells(StringCell(CellKey(3, 1), "x"),
      history1,
      history2,
      plan1,
      StringCell(CellKey(3, 5), "x"))

    assertEquals(List(history1, history2, plan1), table.getSingleRow(TestRowType.Day, Set[TestColType](TestColType.PrevWeek, TestColType.ThisWeek)).toList)
  }


  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBounds: Unit = {
    // Table should not allow cells outside initial bounds.
    table = table.updatedCells(StringCell(CellKey(5, 3), "x"))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBoundsColumn: Unit = {
    // Table should not allow cells outside initial bounds.
    table = table.updatedCells(StringCell(CellKey(1, 6), "x"))
  }

  @Test
  def testSingleColWithEmptyCell: Unit = {

    table = table.updatedCells(StringCell(CellKey(1, 2), "x"),
      foo,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, StringCell(CellKey(2, 1), "")), table.getSingleCol(TestColType.Qualifications, TestRowType.Worker).toList)
  }

  @Test(expected = classOf[RuntimeException])
  def testSingleColWithAmbiguousColumn: Unit = {
    table.getSingleCol(TestColType.PrevWeek, TestRowType.Worker)
  }


}
