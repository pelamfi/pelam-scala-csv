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
 * Base class for cells used in this CSV library.
 *
 * The two main APIs of this library
 * [[fi.pelam.csv.table.TableReader the table oriented]] and the [[fi.pelam.csv.stream.CsvReader stream oriented]]
 * use subclasses of this class to represent the data.
 *
 * One important use for this class is in [[fi.pelam.csv.stream.CsvReader CsvReader]].
 * CsvReader outputs instances of [[fi.pelam.csv.cell.StringCell StringCell]] which is
 * a subclass of this class.

 * @see See the documentation of cellKey member below for design rationale
 *      on having coordinates in each cell.
 */
// @formatter:on
abstract class Cell {

  /**
   * Make a copy of this cell, but with different cell key.
   */
  def updatedCellKey(key: CellKey): Cell

  /**
   * Each cell directly contains information about its coordinates in the CSV data.
   *
   * The rationale is that this makes processing streams of cells simpler and allows
   * for detecting many types of errors. Also when problems are detected the coordinates
   * of the problematic cell can easily be included in the error messages.
   *
   * It is true that in most cases the coordinates of the cell would be known from surrounding
   * data structures. However if we relied solely on that, there would not be an easy uniform
   * way for code operating on cells to know coordinates of each cell.
   *
   * Another downside is that cell instances can't be reused in different places in data
   * in style of the flyweight pattern.
   */
  val cellKey: CellKey

  /**
   * Shortcut to [[CellKey.rowKey]]
   */
  def rowKey = cellKey.rowKey

  /**
   * Shortcut to [[CellKey.colKey]]
   */
  def colKey = cellKey.colKey

  /**
   * Shortcut to index in [[CellKey.rowKey]]
   */
  def rowIndex = cellKey.rowKey.index

  /**
   * Shortcut to index in [[CellKey.colKey]]
   */
  def colIndex = cellKey.colKey.index

  /**
   * The data as it would be represented in CSV file on
   * disk sans quoting.
   *
   * Subclasses of this Cell type should provide more meaningful ways
   * of accessing data.
   *
   * @see [[IntegerCell IntegerCell]]
   */
  def serializedString: String

  /**
   * The data in possibly more refined form than serializedString depending on the subclass
   * of Cell in question.
   *
   * For example IntegerCell returns Int.
   */
  def value: Any

  override def toString() = this.getClass().getSimpleName() + s" with value '$value' at $cellKey"
}

object Cell {
  type ParserResult = Either[CellParsingError, Cell]

  /**
   * A type for client defined [[fi.pelam.csv.cell.Cell Cell]] parsers
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
  type Parser = (CellKey, String) => ParserResult
}
