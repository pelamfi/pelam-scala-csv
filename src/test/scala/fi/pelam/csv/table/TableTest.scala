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

    val updated = table.updatedCells(
      StringCell(CellKey(1, 2), "x"),
      foo,
      bar,
      StringCell(CellKey(3, 1), "x"))

    assertEquals(List(foo, bar), updated.getSingleCol(TestRowType.Worker, TestColType.Qualifications).toList)
  }

  @Test
  def testGetSingleRow: Unit = {

    val updated = table.updatedCells(StringCell(CellKey(3, 1), "x"),
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

    assertEquals(List(foo, StringCell(CellKey(2, 1), "")), updated.getSingleCol(TestRowType.Worker, TestColType.Qualifications).toList)
  }

  @Test(expected = classOf[RuntimeException])
  def testSingleColWithAmbiguousColumn: Unit = {
    table.getSingleCol(TestRowType.Worker, TestColType.PrevWeek)
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

    assertEquals(List("foo","bar"), table.getSingleCol("data", "name").map(_.value).toList)

    assertEquals(List(1,2), table.getSingleCol("data", "number").map(_.value).toList)
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

