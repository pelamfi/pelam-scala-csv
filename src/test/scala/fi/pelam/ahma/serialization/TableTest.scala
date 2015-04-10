package fi.pelam.ahma.serialization

import org.junit.Assert._
import org.junit.Test

import scala.collection.immutable.TreeMap

class TableTest {

  val table = new Table(TreeMap(RowKey(0) -> RowType.CommentRow,
    RowKey(1) -> RowType.Worker,
    RowKey(2) -> RowType.Worker,
    RowKey(3) -> RowType.Day),

    TreeMap(ColKey(1) -> ColType.Types,
      ColKey(2) -> ColType.History,
      ColKey(3) -> ColType.History), List())

  val foo = SimpleCell(CellKey(1, 1), "foo")
  val bar = SimpleCell(CellKey(2, 1), "bar")

  @Test
  def testSingleColTest: Unit = {

    table.setCell(SimpleCell(CellKey(1, 2), "x"))
    table.setCell(foo)
    table.setCell(bar)
    table.setCell(SimpleCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, bar), table.getSingleCol(ColType.Types, RowType.Worker).toList)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBounds: Unit = {
    // Table should not allow cells outside initial bounds.
    table.setCell(SimpleCell(CellKey(4, 3), "x"))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBoundsColumn: Unit = {
    // Table should not allow cells outside initial bounds.
    table.setCell(SimpleCell(CellKey(1, 4), "x"))
  }

  @Test
  def testSingleColWithEmptyCellTest: Unit = {

    table.setCell(SimpleCell(CellKey(1, 2), "x"))
    table.setCell(foo)
    table.setCell(SimpleCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, SimpleCell(CellKey(2, 1), "")), table.getSingleCol(ColType.Types, RowType.Worker).toList)
  }

  @Test(expected = classOf[RuntimeException])
  def testSingleColWithAmbiguousColumn: Unit = {
    table.getSingleCol(ColType.History, RowType.Worker)
  }


}
