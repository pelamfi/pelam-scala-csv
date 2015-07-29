package fi.pelam.csv.stream

import fi.pelam.csv.CsvConstants._
import fi.pelam.csv.cell.{Cell, CellKey}

/**
 * Opposite of [[CsvReader]]. The API is simple. There
 * are just 2 public methods. One to write a single cell and another
 * to write a [[http://www.scala-lang.org/api/current/index.html#scala.collection.TraversableOnce TraversableOnce]]
 * of cells.
 *
 * The cells are expected to be in top to bottom left to right order.
 * The sequence of cells can contain holes. Empty cells in CSV data will
 * be emitted for those.
 *
 * This class handles quoting, but all other special processing
 * is handled by [[fi.pelam.csv.cell.Cell.serializedString]] implementations.
 *
 * @note This class does not flush or close the [[output]] stream.
 * Client must take care of that.
 *
 * @param output Java writer to write the CSV data to.
 * @param separator Optionally specify CSV separator to use.
 */
class CsvWriter(val output: java.io.Writer, val separator: Char = defaultSeparatorChar) {

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
   * Will raise an error if cell with succeeding [[fi.pelam.csv.cell.CellKey CellKey]]
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
   * The cells are expected to be in [[fi.pelam.csv.cell.CellKey]] order.
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
