package fi.pelam.csv.table

import fi.pelam.csv.cell._
import fi.pelam.csv.util.SortedBiMap
import org.junit.Assert._
import org.junit.Test

class TableTest {

  import TableTest._

  @Test
  def testGetSingleCol: Unit = {

    val updated = table.updatedCells(
      StringCell(CellKey(1, 2), "x"),
      foo,
      bar,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, bar), updated.getSingleCol(TestColType.Qualifications, TestRowType.Worker).toList)
  }

  @Test
  def testGetSingleRow: Unit = {

    val updated = table.updatedCells(StringCell(CellKey(3, 1), "x"),
      history1,
      history2,
      plan1,
      StringCell(CellKey(3, 5), "x"))

    assertEquals(List(history1, history2, plan1), updated.getSingleRow(TestRowType.Day, Set[TestColType](TestColType.PrevWeek, TestColType.ThisWeek)).toList)
  }


  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBounds: Unit = {
    // Table should not allow cells outside initial bounds.
    table.updatedCells(StringCell(CellKey(5, 3), "x"))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBoundsColumn: Unit = {
    // Table should not allow cells outside initial bounds.
    table.updatedCells(StringCell(CellKey(1, 6), "x"))
  }

  @Test
  def testSingleColWithEmptyCell: Unit = {

    val updated = table.updatedCells(StringCell(CellKey(1, 2), "x"),
      foo,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, StringCell(CellKey(2, 1), "")), updated.getSingleCol(TestColType.Qualifications, TestRowType.Worker).toList)
  }

  @Test(expected = classOf[RuntimeException])
  def testSingleColWithAmbiguousColumn: Unit = {
    table.getSingleCol(TestColType.PrevWeek, TestRowType.Worker)
  }

}

object TableTest {
  val table: Table[TestRowType, TestColType, SimpleTableMetadata] = Table(
    List[Cell](),
    SortedBiMap[RowKey, TestRowType](RowKey(0) -> TestRowType.CommentRow,
      RowKey(1) -> TestRowType.Worker,
      RowKey(2) -> TestRowType.Worker,
      RowKey(3) -> TestRowType.Day,
      RowKey(4) -> TestRowType.CommentRow),
    SortedBiMap[ColKey, TestColType](ColKey(1) -> TestColType.Qualifications,
      ColKey(2) -> TestColType.PrevWeek,
      ColKey(3) -> TestColType.PrevWeek,
      ColKey(4) -> TestColType.ThisWeek,
      ColKey(5) -> TestColType.CommentCol),
    SimpleTableMetadata())

  val foo = StringCell(CellKey(1, 1), "foo")
  val bar = StringCell(CellKey(2, 1), "bar")
  val history1 = StringCell(CellKey(3, 2), "history1")
  val history2 = StringCell(CellKey(3, 3), "history2")
  val plan1 = StringCell(CellKey(3, 4), "plan1")
}

