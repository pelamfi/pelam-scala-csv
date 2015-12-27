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

package fi.pelam.csv.cell

/**
 * Coordinates of a [[fi.pelam.csv.cell.Cell Cell]] in a CSV "table".
 *
 * Basically a composition of [[fi.pelam.csv.cell.RowKey RowKey]] and [[fi.pelam.csv.cell.ColKey ColKey]]
 * or alternatively just two zero based integers [[rowIndex]] and [[colIndex]].
 *
 * [[fi.pelam.csv.cell.Cell Each Cell]] has a `CellKey` as a member.
 *
 * == Cell coordinates ==
 *
 * Columns and rows are thought to start from top left corner.
 *
 * Typically in spreadsheet programs columns are numbered with an alphabetical
 * scheme and the row number is a one based integer.
 *
 * == Ordering ==
 *
 * CellKeys are ordered first in increasing row order and then in increasing
 * column order.
 *
 * The object representations provide type safety and spread sheet style
 * column and row numbering.
 *
 * @constructor See alternate constructors on the companion object.
 *             Create a new `CellKey` from row index and column index.
 */
final case class CellKey(rowIndex: Int, colIndex: Int) extends Ordered[CellKey] {

  import CellKey._

  /**
   * Method for advancing one row.
   */
  def nextRow: CellKey = CellKey(rowIndex + 1, 0)

  /**
   * Method for advancing one column.
   */
  def nextCol: CellKey = CellKey(rowIndex, colIndex + 1)

  /**
   * Extracts the row key component.
   */
  val rowKey = RowKey(rowIndex)

  /**
   * Extracts the column key component.
   */
  val colKey = ColKey(colIndex)

  /**
   * This is equivalent to `CellKey.unapply(cellKey).get`.
   * @return the `(rowIndex, colIndex)` tuple.
   */
  def indices: IndicesTuple = (rowIndex, colIndex)

  // @formatter:off IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
  /**
   * Example: {{{
   *   scala> CellKey(26, 26)
   *   cellKey: fi.pelam.csv.cell.CellKey = Row 26, Column AA (26)
   * }}}
   */
  // @formatter:on
  override def toString(): String = s"$rowKey, $colKey"

  /**
   * See section on ordering at class top level documentation.
   */
  override def compare(that: CellKey): Int = {
    // Original idea from http://stackoverflow.com/a/19348339/1148030
    indicesTupleOrdering.compare(this.indices, that.indices)
  }
}

object CellKey {
  def apply(rowKey: RowKey, colIndex: Int): CellKey = CellKey(rowKey.index, colIndex)

  def apply(rowIndex: Int, colKey: ColKey): CellKey = CellKey(rowIndex, colKey.index)

  def apply(rowKey: RowKey, colKey: ColKey): CellKey = CellKey(rowKey.index, colKey.index)

  type IndicesTuple = (Int, Int)

  private val indicesTupleOrdering = implicitly(Ordering[IndicesTuple])

  val invalid = CellKey(Int.MinValue / 2, Int.MinValue / 2)

}
