package fi.pelam.ahma.serialization

case class CellKey(rowIndex: Int, colIndex: Int) {

  def row = RowKey(rowIndex)

  def col = ColKey(colIndex)
}
