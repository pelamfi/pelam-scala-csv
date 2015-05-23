package fi.pelam.ahma.serialization

class CsvWriter(val output: java.io.Writer, val separator: Char = ',') {

  var col = 0
  var row = 0

  def write(cell: Cell): Unit = {
    val cellSerialized = cell.serializedString
    val quotingNeeded = cellSerialized.contains(separator)
  }

  def write(cells: Traversable[Cell]): Unit = cells.foreach(write)

}
