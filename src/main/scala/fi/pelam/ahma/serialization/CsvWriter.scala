package fi.pelam.ahma.serialization

import fi.pelam.ahma.serialization.CsvConstants._

class CsvWriter(val output: java.io.Writer, val separator: Char = defaultSeparatorChar) {

  import CsvWriter._

  private[this] var _lastWriteKey: Option[CellKey] = None
  private[this] var _position = CellKey(0, 0)

  def positionKey: CellKey = _position

  def lastWriteKey: Option[CellKey] = _lastWriteKey

  def goToNextRow() = {
    output.append("\n")
    _position = positionKey.nextRow
  }

  def goToNextCol() = {
    output.append(separator)
    _position = positionKey.nextCol
  }

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

  def write(cells: Traversable[Cell]): Unit = cells.foreach(write)

}

object CsvWriter {

  def serialize(cell: Cell, separator: Char): String = {
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
