package fi.pelam.ahma.serialization

import com.google.common.base.Charsets
import fi.pelam.ahma.localization.AhmaLocalization
import org.junit.Assert._
import org.junit.Test

import scala.collection.immutable.TreeMap

object TableTest {
  def makeTable() = new Table(Charsets.UTF_8,
    CsvConstants.defaultSeparatorChar,
    AhmaLocalization.localeEn,
    AhmaLocalization.localeEn,
    TreeMap(RowKey(0) -> RowType.CommentRow,
      RowKey(1) -> RowType.Worker,
      RowKey(2) -> RowType.Worker,
      RowKey(3) -> RowType.Day,
      RowKey(4) -> RowType.CommentRow),

    TreeMap(ColKey(1) -> ColType.Types,
      ColKey(2) -> ColType.History,
      ColKey(3) -> ColType.History,
      ColKey(4) -> ColType.Plan,
      ColKey(5) -> ColType.CommentCol), List())

  val foo = StringCell(CellKey(1, 1), "foo")
  val bar = StringCell(CellKey(2, 1), "bar")
  val history1 = StringCell(CellKey(3, 2), "history1")
  val history2 = StringCell(CellKey(3, 3), "history2")
  val plan1 = StringCell(CellKey(3, 4), "plan1")
}

class TableTest {

  import TableTest._

  val table = makeTable()

  @Test
  def testGetSingleCol: Unit = {

    table.setCell(StringCell(CellKey(1, 2), "x"))
    table.setCell(foo)
    table.setCell(bar)
    table.setCell(StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, bar), table.getSingleCol(ColType.Types, RowType.Worker).toList)
  }

  @Test
  def testGetSingleRow: Unit = {

    table.setCell(StringCell(CellKey(3, 1), "x"))
    table.setCell(history1)
    table.setCell(history2)
    table.setCell(plan1)
    table.setCell(StringCell(CellKey(3, 5), "x"))

    assertEquals(List(history1, history2, plan1), table.getSingleRow(RowType.Day, Set[ColType](ColType.History, ColType.Plan)).toList)
  }


  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBounds: Unit = {
    // Table should not allow cells outside initial bounds.
    table.setCell(StringCell(CellKey(5, 3), "x"))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBoundsColumn: Unit = {
    // Table should not allow cells outside initial bounds.
    table.setCell(StringCell(CellKey(1, 6), "x"))
  }

  @Test
  def testSingleColWithEmptyCell: Unit = {

    table.setCell(StringCell(CellKey(1, 2), "x"))
    table.setCell(foo)
    table.setCell(StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, StringCell(CellKey(2, 1), "")), table.getSingleCol(ColType.Types, RowType.Worker).toList)
  }

  @Test(expected = classOf[RuntimeException])
  def testSingleColWithAmbiguousColumn: Unit = {
    table.getSingleCol(ColType.History, RowType.Worker)
  }


}
