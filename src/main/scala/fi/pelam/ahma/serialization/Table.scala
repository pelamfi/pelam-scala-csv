package fi.pelam.ahma.serialization

import java.util.ResourceBundle

import scala.collection.mutable.HashMap

class Table {

  private[this] var resourceBundle: ResourceBundle = null

  private[this] var rowCountPrivate: Int = 0

  private[this] var colCountPrivate: Int = 0

  private[this] val cellMap = new HashMap[CellKey, Cell]

  private[this] val rowType = new HashMap[RowKey, RowType]

  private[this] val colType = new HashMap[ColKey, ColType]

  def rowCount = rowCountPrivate

  def colCount = colCountPrivate

  val rowTypeCol = ColKey(0)

  def addCells(cells: TraversableOnce[SimpleCell]) = {
    for (cell <- cells) {
      val key = cell.cellKey

      rowCountPrivate = Math.max(rowCountPrivate, key.rowIndex + 1)
      colCountPrivate = Math.max(colCountPrivate, key.colIndex + 1)

      if (key.col == rowTypeCol) {

      }

      cellMap(key) = cell
    }
  }

  def setCell(key: CellKey, cell: Cell) = ???

  def setCells(rowKey: RowKey, colType: ColType, cell: IndexedSeq[Cell]) = ???

  def setCells(colKey: ColKey, rowType: RowType, cell: IndexedSeq[Cell]) = ???

  def getCells(key: RowKey, colType: ColType): IndexedSeq[Cell] = ???

  def getCells(key: ColKey, rowType: RowType): IndexedSeq[Cell] = ???

  def setRowType(key: RowKey, rowType: RowType) = ???

  def setColType(key: RowKey, rowType: ColType) = ???

  def getRowType(key: RowKey): Unit = ???

}
