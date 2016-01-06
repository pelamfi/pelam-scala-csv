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

import fi.pelam.csv.cell._
import fi.pelam.csv.util.SortedBiMap
import org.junit.Assert._
import org.junit.Test

class TableTest {

  import TableTest._

  @Test
  def testGetSingleCol: Unit = {

    val updated = emptyTypedTable.updatedCells(
      StringCell(CellKey(1, 2), "x"),
      cell2b,
      cell3b,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(cell2b, cell3b), updated.getSingleCol(TestRowType.Worker, TestColType.Qualifications).toList)
  }

  @Test
  def testGetSingleRow: Unit = {

    val updated = emptyTypedTable.updatedCells(StringCell(CellKey(3, 1), "x"),
      history1,
      history2,
      plan1,
      StringCell(CellKey(3, 5), "x"))

    val requiredColTypes = Set[TestColType](TestColType.PrevWeek, TestColType.ThisWeek)

    assertEquals(List(history1, history2, plan1), updated.getSingleRow(TestRowType.Day, requiredColTypes).toList)
  }


  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBounds: Unit = {
    // Table should not allow cells outside initial bounds.
    emptyTypedTable.updatedCells(StringCell(CellKey(6, 4), "x"))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testSetCellOutsideBoundsColumn: Unit = {
    // Table should not allow cells outside initial bounds.
    emptyTypedTable.updatedCells(StringCell(CellKey(2, 7), "x"))
  }

  @Test
  def testSingleColWithEmptyCell: Unit = {

    val updated = emptyTypedTable.updatedCells(StringCell(CellKey(1, 2), "x"),
      cell2b,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(cell2b, StringCell(CellKey(2, 1), "")), updated.getSingleCol(TestRowType.Worker, TestColType.Qualifications).toList)
  }

  @Test(expected = classOf[RuntimeException])
  def testSingleColWithAmbiguousColumn: Unit = {
    emptyTypedTable.getSingleCol(TestRowType.Worker, TestColType.PrevWeek)
  }

  @Test
  def testTableScaladocExample = {

    // This test should match the code in the Table class ScalaDoc example.

    val table = Table(
      List(StringCell(CellKey(0, 0), "name"),
        StringCell(CellKey(0, 1), "value"),
        StringCell(CellKey(1, 0), "foo"),
        IntegerCell(CellKey(1, 1), 1),
        StringCell(CellKey(2, 0), "bar"),
        IntegerCell(CellKey(2, 1), 2)
      ),

      SortedBiMap(RowKey(0) -> "header",
        RowKey(1) -> "data",
        RowKey(2) -> "data"),

      SortedBiMap(ColKey(0) -> "name",
        ColKey(1) -> "number")
    )

    assertEquals(List("foo", "bar"), table.getSingleCol("data", "name").map(_.value).toList)

    assertEquals(List(1, 2), table.getSingleCol("data", "number").map(_.value).toList)
  }

  @Test
  def testTableConstructionWithHoles = {
    val table = Table(
      List(StringCell(CellKey(0, 0), "a"),
        StringCell(CellKey(0, 1), "b"),
        StringCell(CellKey(1, 1), "d")
      )
    )

    assertEquals("The cell at 0,0 should be empty string",
      "'a' 'b' '' 'd'",
      table.getCells().map("'" + _.value + "'").reduce(_ + " " + _))
  }

  @Test
  def testUpdatedRegionOneCell = {
    import Table._

    val replacement = StringCell(cell2b.cellKey, "replaced")

    val result = testTable.updatedRegion(IndexedSeq(cell2b), IndexedSeq(replacement))

    assertEquals(testTable.updatedCells(replacement), result)
  }

  @Test
  def testUpdatedCell = {
    val replacement = StringCell(cell2b.cellKey, "replaced")

    val result = testTable.updatedCells(IndexedSeq(replacement))

    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,,,,\n" +
      "Row 2/Worker:,replaced,2c,,,,,\n" +
      "Row 3/Worker:,3b,,,,,,\n" +
      "Row 4/Day:,4b,,,,,,\n" +
      "Row 5/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 6/CommentRow:,,,,,,,\n",
      result.toString())
  }

  @Test
  def testRemoveRows = {
    val result = testTable.resizeRows(cell3b.cellKey.rowKey, -1)

    assertEquals("One worker row should be gone",
      "columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
        "Row 1/CommentRow:,,,,,,,\n" +
        "Row 2/Worker:,2b,2c,,,,,\n" +
        "Row 3/Day:,4b,,,,,,\n" +
        "Row 4/:5a-untyped,,,,,5f-untyped,,\n" +
        "Row 5/CommentRow:,,,,,,,\n",
      result.toString())

    assertEquals(4, result.rowTypes.size)
  }

  @Test
  def testAddRows = {
    val result = testTable.resizeRows(cell3b.cellKey.rowKey, 1, cellKey => StringCell(cellKey, "x"))

    assertEquals("One worker row should be added after 2 existing.",
      "columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
        "Row 1/CommentRow:,,,,,,,\n" +
        "Row 2/Worker:,2b,2c,,,,,\n" +
        "Row 3/Worker:,3b,,,,,,\n" +
        "Row 4/Worker:x,x,x,x,x,x,x,\n" +
        "Row 5/Day:,4b,,,,,,\n" +
        "Row 6/:5a-untyped,,,,,5f-untyped,,\n" +
        "Row 7/CommentRow:,,,,,,,\n",
      result.toString())
  }

  @Test
  def testUpdatedRegion1Cell = {
    import Table._
    val replacement = StringCell(cell2b.cellKey, "replaced")

    val result = testTable.updatedRegion(IndexedSeq(cell2b), IndexedSeq(replacement))

    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,,,,\n" +
      "Row 2/Worker:,replaced,2c,,,,,\n" +
      "Row 3/Worker:,3b,,,,,,\n" +
      "Row 4/Day:,4b,,,,,,\n" +
      "Row 5/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 6/CommentRow:,,,,,,,\n",
      result.toString())
  }

  @Test
  def testUpdatedRegionToZero = {
    import Table._

    val result = testTable.updatedRegion(IndexedSeq(cell2b), IndexedSeq())

    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,,,,\n" +
      "Row 2/Worker:,3b,,,,,,\n" +
      "Row 3/Day:,4b,,,,,,\n" +
      "Row 4/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 5/CommentRow:,,,,,,,\n",
      result.toString())
  }

  @Test
  def testUpdatedRegionSmaller = {
    import Table._
    val replacement = StringCell(cell2b.cellKey, "replaced")

    val result = testTable.updatedRegion(IndexedSeq(cell2b, cell3b), IndexedSeq(replacement))

    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,,,,\n" +
      "Row 2/Worker:,replaced,2c,,,,,\n" +
      "Row 3/Day:,4b,,,,,,\n" +
      "Row 4/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 5/CommentRow:,,,,,,,\n",
      result.toString())
  }

  @Test
  def testUpdatedRegionOutside = {
    import Table._

    val replacement = StringCell(cell4b.cellKey, "new")

    val result = testTable.updatedRegion(IndexedSeq(cell2b, cell3b), IndexedSeq(replacement))

    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,,,,\n" +
      "Row 2/Worker:,2b,2c,,,,,\n" +
      "Row 3/Worker:,3b,,,,,,\n" +
      "Row 4/Worker:,new,,,,,,\n" +
      "Row 5/Day:,4b,,,,,,\n" +
      "Row 6/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 7/CommentRow:,,,,,,,\n",
      result.toString())
  }

  @Test
  def testToString = {
    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,,,,\n" +
      "Row 2/Worker:,2b,2c,,,,,\n" +
      "Row 3/Worker:,3b,,,,,,\n" +
      "Row 4/Day:,4b,,,,,,\n" +
      "Row 5/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 6/CommentRow:,,,,,,,\n", testTable.toString())
  }

  @Test
  def testToStringTypedCells = {
    assertEquals("columns:,Qualifications,PrevWeek,PrevWeek,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:i 123,d 123.0,,,,,,\n" +
      "Row 2/Worker:,2b,2c,,,,,\n" +
      "Row 3/Worker:,3b,,,,,,\n" +
      "Row 4/Day:,4b,,,,,,\n" +
      "Row 5/:5a-untyped,,,,,5f-untyped,,\n" +
      "Row 6/CommentRow:,,,,,,,\n", testTableTypedCells.toString())
  }

  @Test
  def testGetRows = {
    val resultAsString: String = Table.rowsToString(testTable.getRows(TestRowType.Worker))

    assertEquals("Row 2:,2b,2c,,,,,\n" +
      "Row 3:,3b,,,,,,\n",
      resultAsString)
  }

  @Test
  def testSpannedRegion = {
    assertEquals((CellKey(1, 1), CellKey(2, 2)), Table.spannedRegion(IndexedSeq(cell2b)))
  }

  @Test
  def testSpannedRegion2 = {
    assertEquals((CellKey(1, 1), CellKey(2, 3)), Table.spannedRegion(IndexedSeq(cell2b, cell2c)))
  }

  @Test
  def testSpannedRegion2Down = {
    assertEquals((CellKey(1, 1), CellKey(3, 2)), Table.spannedRegion(IndexedSeq(cell2b, cell3b)))
  }

  @Test
  def testSpannedRegionEmpty = {
    assertEquals((CellKey(0, 0), CellKey(0, 0)), Table.spannedRegion(IndexedSeq()))
  }

}

object TableTest {
  val emptyTypedTable: Table[TestRowType, TestColType, SimpleMetadata] = Table(
    List[Cell](),
    SortedBiMap[RowKey, TestRowType](RowKey(0) -> TestRowType.CommentRow,
      RowKey(1) -> TestRowType.Worker,
      RowKey(2) -> TestRowType.Worker,
      RowKey(3) -> TestRowType.Day,
      RowKey(5) -> TestRowType.CommentRow),
    SortedBiMap[ColKey, TestColType](ColKey(1) -> TestColType.Qualifications,
      ColKey(2) -> TestColType.PrevWeek,
      ColKey(3) -> TestColType.PrevWeek,
      ColKey(4) -> TestColType.ThisWeek,
      ColKey(6) -> TestColType.CommentCol),
    SimpleMetadata())

  val cell2b = StringCell(CellKey(1, 1), "2b")
  val cell3b = StringCell(CellKey(2, 1), "3b")
  val cell3c = StringCell(CellKey(2, 2), "3c")
  val history1 = StringCell(CellKey(3, 2), "history1")
  val history2 = StringCell(CellKey(3, 3), "history2")
  val plan1 = StringCell(CellKey(3, 4), "plan1")

  val cell2c = StringCell(CellKey(1, 2), "2c")
  val cell4b = StringCell(CellKey(3, 1), "4b")
  val cell4c = StringCell(CellKey(3, 2), "4c")
  val cellUntyped1 = StringCell(CellKey(4, 0), "5a-untyped")
  val cellUntyped2 = StringCell(CellKey(4, 5), "5f-untyped")

  val testTable = emptyTypedTable.updatedCells(cell2c, cell2b, cell3b, cell4b, cellUntyped1, cellUntyped2)

  val testTableTypedCells = testTable
    .updatedCells(IntegerCell(CellKey(0, 0), 123))
    .updatedCells(DoubleCell(CellKey(0, 1), 123))
}

