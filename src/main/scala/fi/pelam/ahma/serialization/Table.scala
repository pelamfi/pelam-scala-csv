package fi.pelam.ahma.serialization

import java.util.ResourceBundle

import scala.collection.SortedMap
import scala.collection.mutable.HashMap

object Table {
  val rowTypeCol = ColKey(0)
}

class Table(val rowTypes: SortedMap[RowKey, RowType], val colTypes: SortedMap[ColKey, ColType]) {

  // http://stackoverflow.com/a/24222250/1148030
  val rowsByType = rowTypes.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq)

  val colsByType = colTypes.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq)

  private[this] var resourceBundle: ResourceBundle = null

  private[this] var rowCountPrivate: Int = 0

  private[this] var colCountPrivate: Int = 0

  private[this] val cellMap = new HashMap[CellKey, Cell]

  def rowCount = rowCountPrivate

  def colCount = colCountPrivate

  def setCells(cells: TraversableOnce[SimpleCell]) = {
    for (cell <- cells) {
      setCell(cell)
    }
  }

  def setCell(cell: Cell) = {
    val key = cell.cellKey

    rowCountPrivate = Math.max(rowCountPrivate, key.rowIndex + 1)
    colCountPrivate = Math.max(colCountPrivate, key.colIndex + 1)

    cellMap(key) = cell
  }

  def getCells(rowKey: RowKey): IndexedSeq[Option[Cell]] = {
    for (i <- 0 until colCount) yield cellMap.get(CellKey(rowKey, i))
  }

  def getCells(colKey: ColKey): IndexedSeq[Option[Cell]] = {
    for (i <- 0 until rowCount) yield cellMap.get(CellKey(i, colKey))
  }

  def mapCellKeys[T](rowKey: RowKey)(func: (CellKey) => T): IndexedSeq[T] = {
    for (i <- 0 until colCount) yield func(CellKey(rowKey, i))
  }

  def mapCellKeys[T](colKey: ColKey)(func: (CellKey) => T): IndexedSeq[T] = {
    for (i <- 0 until rowCount) yield func(CellKey(i, colKey))
  }

  def getCellKeys(colKey: ColKey): IndexedSeq[CellKey] = {
    for (i <- 0 until rowCount) yield CellKey(i, colKey)
  }


  /**
   * Throws if the number of columns with given type is not 1
   */
  def getSingleColByType(colType: ColType) = {
    val cols = colsByType(colType)
    if (cols.size == 0) {
      sys.error(s"Expected 1 column of type $colType but no columns found.")
    } else if (cols.size > 1) {
      sys.error(s"Expected 1 column of type $colType but more than 1 found.")
    } else {
      cols(0)
    }
  }

  /**
   * Get cells from single column of colType for each row of rowType.
   *
   * Throws if there are multiple columns with ColType
   */
  def getSingleCol(colType: ColType, rowType: RowType): IndexedSeq[Option[Cell]] = {
    val colKey = getSingleColByType(colType)

    for (cellKey <- getCellKeys(colKey);
         if rowTypes(cellKey.rowKey) == rowType) yield {
      cellMap.get(cellKey)
    }
  }


}
