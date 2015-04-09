package fi.pelam.ahma.serialization

import org.junit.Assert._
import org.junit.Test

import scala.collection.immutable.TreeMap

class TableTest {

  val table = new Table(TreeMap(RowKey(0) -> RowType.Comment,
    RowKey(1) -> RowType.Worker,
    RowKey(2) -> RowType.Worker,
    RowKey(3) -> RowType.Day), TreeMap(ColKey(1) -> ColType.Types))

  val foo = SimpleCell(CellKey(1, 1), "foo")
  val bar = SimpleCell(CellKey(2, 1), "bar")

  @Test
  def testSingleColTest: Unit = {

    table.setCell(SimpleCell(CellKey(1, 2), "x"))
    table.setCell(foo)
    table.setCell(bar)
    table.setCell(SimpleCell(CellKey(3, 1), "x"))

    assertEquals(List(Some(foo), Some(bar)), table.getSingleCol(ColType.Types, RowType.Worker).toList)
  }

  @Test
  def testSingleColWithEmptyCellTest: Unit = {

    table.setCell(SimpleCell(CellKey(1, 2), "x"))
    table.setCell(foo)
    table.setCell(SimpleCell(CellKey(3, 1), "x"))

    assertEquals(List(Some(foo), None), table.getSingleCol(ColType.Types, RowType.Worker).toList)
  }

}
