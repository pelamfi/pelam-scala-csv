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

// @formatter:off IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
/**
 * Basically this class is a sample implementation of a more specialised subtype of
 * [[fi.pelam.csv.cell.Cell]].
 *
 * It is expected that any nontrivial client will want to specify its own subtypes
 * of [[fi.pelam.csv.cell.Cell]].
 *
 * This class is quite simple, but the companion object is more
 * interesting as it provides [[Cell.Parser]] functions
 * which produce IntegerCell instances (or errors if parsing fails) from String data.
 *
 * @param cellKey the location of this cell in a CSV table.
 * @param formatter A function used to convert the integer held by this cell into a `String`
 *                  to be stored in CSV text data.
 * @param value is the integer stored in CSV.
 */
// @formatter:on IntelliJ 14.1 (Scala plugin) formatter messes up ScalaDoc
final case class IntegerCell(override val cellKey: CellKey,
  override val value: Int)
  (implicit override val formatter: IntegerCell.NumberFormatter = IntegerCell.defaultFormatter)
  extends Cell with NumberCell[Int] {

  override def updatedCellKey(cellKey: CellKey) = copy(cellKey = cellKey)

  /**
   * Shorter version of `toString` to be used in debug table outputs.
   * Should identify cell type and value in small amount of text.
   */
  override def shortString(): String = "i " + value
}

/**
 * The IntegerCell class it self is quite simple, but this companion object is more
 * interesting as provides [[Cell.Parser]] functions. These functions in turn
 * produce IntegerCell instances (or errors if parsing fails) from String data.
 *
 * [[Cell.Parser]] functions can be used used to upgrade cells in [[fi.pelam.csv.table.TableReader]]
 * in an easy way by using them in a map passed to [[fi.pelam.csv.table.TableReaderConfig.makeCellUpgrader]].
 * The map specifies which cells should be interpreted as containing integers.
 */
object IntegerCell extends PrimitiveCellObject[Int] {
  override val primitiveDescription: String = "integer"

  override def numberToCell(cellKey: CellKey, input: String, number: Number, formatter: NumberFormatter) = {
      val intValue = number.intValue()

      if (intValue == number) {
        Right(IntegerCell(cellKey, intValue)(formatter))
      } else {
        Left(CellParsingError(s"Expected $primitiveDescription, but value '$input' is decimal."))
      }
  }
}


