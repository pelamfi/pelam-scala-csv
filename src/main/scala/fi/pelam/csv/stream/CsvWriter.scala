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

package fi.pelam.csv.stream

import fi.pelam.csv.CsvConstants._
import fi.pelam.csv.cell.{Cell, CellKey}

/**
 * Opposite of [[CsvReader]]. The API is simple. There
 * are just 3 public methods. One to write a single cell and another
 * to write a
 * [[http://www.scala-lang.org/api/current/index.html#scala.collection.TraversableOnce TraversableOnce]]
 * of cells.
 *
 * The third method `goToNextRow` should be called last after the last cell to write the line
 * feed at the end of the file.
 *
 * The cells are expected to be in top to bottom left to right order.
 * The sequence of cells can contain holes. Empty cells in CSV data will
 * be emitted for those.
 *
 * This class handles quoting, but all other special processing
 * is handled by [[fi.pelam.csv.cell.Cell.serializedString]] implementations.
 *
 * @note This class does not flush or close the [[output]] stream.
 *       Client must take care of that.
 *
 * @param output Java writer to write the CSV data to.
 * @param separator Optionally specify CSV separator to use.
 */
final class CsvWriter(val output: java.io.Writer, val separator: Char = defaultSeparatorChar) {

  import CsvWriter._

  private[this] var _lastWriteKey: Option[CellKey] = None
  private[this] var _position = CellKey(0, 0)

  private[this] def positionKey: CellKey = _position

  private[this] def lastWriteKey: Option[CellKey] = _lastWriteKey

  /**
   * This method can be used to force final line feed to CSV file that some programs
   * might expect.
   *
   * If emitted between cells keyed to successive rows, this has no effect.
   *
   */
  def goToNextRow() = {
    output.append("\n")
    _position = positionKey.nextRow
  }

  private[this] def goToNextCol() = {
    output.append(separator)
    _position = positionKey.nextCol
  }

  /**
   * Write a single cell to output stream.
   *
   * Will throw a `RuntimeException` if cell with succeeding [[fi.pelam.csv.cell.CellKey CellKey]]
   * has already been written. This is because cells are expected
   * to be written in natural order of [[fi.pelam.csv.cell.CellKey CellKeys]] (rows top down, columns right to left).
   *
   * @param cell to write.
   */
  def write(cell: Cell): Unit = {
    val key = cell.cellKey

    for (lastWriteKey <- lastWriteKey;
         if key <= lastWriteKey) {
      sys.error(s"Already wrote cell $lastWriteKey and now asked to write $cell")
    }

    while (positionKey.rowIndex < key.rowIndex) {
      goToNextRow()
    }

    while (positionKey.colIndex < key.colIndex) {
      goToNextCol()
    }

    _lastWriteKey = Some(key)

    output.append(serialize(cell, separator))
  }

  /**
   * Shortcut to call write for each cell in a traversable.
   *
   * The cells are expected to be in [[fi.pelam.csv.cell.CellKey CellKey]] order.
   *
   * @param cells sequence of cells to be written.
   */
  def write(cells: TraversableOnce[Cell]): Unit = cells.foreach(write)

}

object CsvWriter {

  /**
   * Handle quoting for a string to be written to CSV data.
   */
  private[csv] def serialize(cell: Cell, separator: Char): String = {
    val cellSerialized = cell.serializedString

    val quotingNeeded = cellSerialized.contains(separator) || cellSerialized.contains(quoteChar)

    val quoted = if (quotingNeeded) {
      quote + cellSerialized.replace(quote, doubleQuote) + quote
    } else {
      cellSerialized
    }

    quoted
  }

}
