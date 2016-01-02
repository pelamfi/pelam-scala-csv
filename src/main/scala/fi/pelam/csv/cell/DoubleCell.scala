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
 * See documentation on [[IntegerCell]]. This is basically same, but with `Double` instead of Int.
 */
final case class DoubleCell(override val cellKey: CellKey,
  override val value: Double)
  (implicit val formatter: DoubleCell.NumberFormatter = DoubleCell.defaultFormatter)
  extends Cell {

  override def updatedCellKey(cellKey: CellKey) = copy(cellKey = cellKey)

  def serializedString: String = {
    formatter(value)
  }

  /**
   * Shorter version of `toString` to be used in debug table outputs.
   * Should identify cell type and value in small amount of text.
   */
  override def shortString(): String = "d " + value
}

/**
 * See documentation on [[IntegerCell]]. This is basically same, but with `Double` instead of Int.
 */
object DoubleCell extends PrimitiveCellObject[Double] {

  override val primitiveDescription = "decimal number (double precision)"

  override def numberToCell(cellKey: CellKey, input: String, number: Number, formatter: NumberFormatter) = {
    Right(DoubleCell(cellKey, number.doubleValue())(formatter))
  }
}

