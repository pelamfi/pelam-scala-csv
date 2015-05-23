package fi.pelam.ahma.serialization

import com.google.common.collect.ComparisonChain

object CellKey {
  def apply(rowKey: RowKey, colIndex: Int): CellKey = CellKey(rowKey.index, colIndex)

  def apply(rowIndex: Int, colKey: ColKey): CellKey = CellKey(rowIndex, colKey.index)

  def apply(rowKey: RowKey, colKey: ColKey): CellKey = CellKey(rowKey.index, colKey.index)
}

case class CellKey(rowIndex: Int, colIndex: Int) extends Ordered[CellKey] {
  def nextRow: CellKey = CellKey(rowIndex + 1, 0)

  def nextCol: CellKey = CellKey(rowIndex, colIndex + 1)

  val rowKey = RowKey(rowIndex)

  val colKey = ColKey(colIndex)

  def indices = (rowIndex, colIndex)

  override def toString(): String = s"$rowKey, $colKey"

  override def compare(that: CellKey): Int = {
    ComparisonChain.start()
      .compare(rowIndex, that.rowIndex)
      .compare(colIndex, that.colIndex)
      .result()
  }
}
