package fi.pelam.ahma.serialization

object CellKey {
  def apply(rowKey: RowKey, colIndex: Int): CellKey = CellKey(rowKey.index, colIndex)

  def apply(rowIndex: Int, colKey: ColKey): CellKey = CellKey(rowIndex, colKey.index)

  def apply(rowKey: RowKey, colKey: ColKey): CellKey = CellKey(rowKey.index, colKey.index)
}

case class CellKey(rowIndex: Int, colIndex: Int) {

  def rowKey = RowKey(rowIndex)

  def colKey = ColKey(colIndex)
}
