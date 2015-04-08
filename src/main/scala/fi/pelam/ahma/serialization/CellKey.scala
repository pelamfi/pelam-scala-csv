package fi.pelam.ahma.serialization

case class CellKey(rowIndex: Int, colIndex: Int) {

  def rowKey = RowKey(rowIndex)

  def colKey = ColKey(colIndex)
}
