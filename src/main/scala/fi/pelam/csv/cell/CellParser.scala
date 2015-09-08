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
 * A base class for client defined [[fi.pelam.csv.cell.Cell Cell]] parsers
 * which construct cells from strings.
 *
 * == Use of this class in [[fi.pelam.csv.table.TableReader TableReader]] ==
 *
 * These parser are used in [[fi.pelam.csv.table.TableReader TableReader]].
 * The parsers are invoked based on [[fi.pelam.csv.table.CellType CellTypes]] of each cell.
 *
 * The parser can then replace the cell with an instance of another subclass of [[fi.pelam.csv.cell.Cell Cell]].
 * The purpose of the mechanism in [[fi.pelam.csv.table.TableReader TableReader]] is to upgrade the
 * cells from simple [[fi.pelam.csv.cell.StringCell StringCell]]s to more specialized subclasses of
 * [[fi.pelam.csv.cell.Cell Cell]] like the [[fi.pelam.csv.cell.IntegerCell IntegerCell]].
 *
 * Another function that can be performed at the same time is validating that the contents of the cells against
 * client specified requirements.
 */
abstract class CellParser {
  def parse(cellKey: CellKey, input: String): Either[CellParsingError, Cell]
}
