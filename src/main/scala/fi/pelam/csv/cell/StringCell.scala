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
 * The most basic subtype of [[fi.pelam.csv.cell.Cell Cell]].
 *
 * This cell type simply contains the raw data from a position in a CSV text file.
 *
 * @note However note that any possible quoting is removed and no separator or CSV line ending
 *       characters are included.
 *
 *       The lower level CSV parser API [[fi.pelam.csv.stream.CsvReader CsvReader]] produces these.
 *
 * @param cellKey the location of the cell in the CSV file.
 * @param serializedString simply the string from the CSV file.
 */
// @formatter:on IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
final case class StringCell(override val cellKey: CellKey,
  override val serializedString: String)
  extends Cell {

  /**
   * This cell type does not interpret the data contained in the raw CSV cell it represents
   * in any way.
   * @return same as `serializedString`
   */
  override def value = serializedString


}

object StringCell {

  val parser: Cell.Parser = { (cellKey: CellKey, input: String) =>
    Right(StringCell(cellKey, input))
  }

}
