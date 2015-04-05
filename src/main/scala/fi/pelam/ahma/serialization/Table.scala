package fi.pelam.ahma.serialization

import scala.collection.mutable.HashMap

class Table {

  private[this] var maxRow: Int = 0

  private[this] var maxCol: Int = 0

  private[this] val cellMap = new HashMap[CellKey, Cell]

  private[this] val rowType = new HashMap[RowKey, RowType]

  private[this] val colType = new HashMap[ColKey, ColType]

  def setCell(key: CellKey, cell: Cell) = ???

  def setCells(rowKey: RowKey, colType: ColType, cell: IndexedSeq[Cell]) = ???

  def setCells(colKey: ColKey, rowType: RowType, cell: IndexedSeq[Cell]) = ???

  def getCells(key: RowKey, colType: ColType): IndexedSeq[Cell] = ???

  def getCells(key: ColKey, rowType: RowType): IndexedSeq[Cell] = ???

  def setRowType(key: RowKey, rowType: RowType) = ???

  def setColType(key: RowKey, rowType: ColType) = ???

}
