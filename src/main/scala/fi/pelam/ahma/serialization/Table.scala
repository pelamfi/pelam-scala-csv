package fi.pelam.ahma.serialization

import java.util.{Locale, ResourceBundle}

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

object Table {
  val rowTypeCol = ColKey(0)

  // http://stackoverflow.com/a/24222250/1148030
  def reverseMap[A, B](map: scala.collection.Map[A, B]): Map[B, IndexedSeq[A]] = map.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq)

  def reverseMapSorted[A, B <: Ordered[B]](map: Map[A, B]): SortedMap[B, IndexedSeq[A]] =
    TreeMap[B, IndexedSeq[A]]() ++ reverseMap(map)

}

class Table(val locale: Locale, val rowTypes: SortedMap[RowKey, RowType],
  val colTypes: SortedMap[ColKey, ColType],
  initialCells: TraversableOnce[Cell]) {

  import fi.pelam.ahma.serialization.Table._

  val rowsByType: Map[RowType, IndexedSeq[RowKey]] = reverseMap(rowTypes)

  val colsByType: Map[ColType, IndexedSeq[ColKey]] = reverseMap(colTypes)

  private[this] var resourceBundle: ResourceBundle = null

  val rowCount: Int = rowTypes.keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  val colCount: Int = colTypes.keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  private[this] val cells: Array[Array[Cell]] = {
    val initialCellMap = initialCells.map(cell => cell.cellKey -> cell).toMap

    val rowArray = new Array[Array[Cell]](rowCount)

    for (rowIndex <- 0 until rowCount) {

      val rowKey = RowKey(rowIndex)
      val colArray = new Array[Cell](colCount)

      rowArray(rowIndex) = colArray

      for (colIndex <- 0 until colCount) {

        val cellKey = CellKey(rowKey, colIndex)
        val cell = initialCellMap.get(cellKey).getOrElse(new SimpleCell(cellKey, ""))

        colArray(colIndex) = cell
      }

    }

    rowArray
  }

  def setCells(cells: TraversableOnce[Cell]) = {
    for (cell <- cells) {
      setCell(cell)
    }
  }

  def setCell(cell: Cell) = {
    val key = cell.cellKey

    if (key.rowIndex >= rowCount) {
      throw new IllegalArgumentException(s"Row number ${key.rowIndex + 1} is outside the number of rows $rowCount. Mark the row a comment?")
    }

    if (key.colIndex >= colCount) {
      throw new IllegalArgumentException(s"Column number ${key.colIndex + 1} is outside the number of columns $colCount. Mark the column a comment?")
    }

    cells(key.rowIndex)(key.colIndex) = cell
  }

  def getCell(key: CellKey) = cells(key.rowIndex)(key.colIndex)

  def getCells(rowKey: RowKey): IndexedSeq[Cell] = {
    for (i <- 0 until colCount) yield cells(rowKey.index)(i)
  }

  def getCells(colKey: ColKey): IndexedSeq[Cell] = {
    for (i <- 0 until rowCount) yield cells(i)(colKey.index)
  }

  def getCellKeys(colKey: ColKey): IndexedSeq[CellKey] = {
    for (i <- 0 until rowCount) yield CellKey(i, colKey)
  }

  def getCellKeys(rowKey: RowKey): IndexedSeq[CellKey] = {
    for (i <- 0 until colCount) yield CellKey(rowKey, i)
  }

  /**
   * Throws if the number of columns with given type is not 1
   */
  def getSingleColByType(colType: ColType): ColKey = {
    val cols = colsByType(colType)
    if (cols.size == 0) {
      sys.error(s"Expected 1 column of type $colType but no columns of that type found.")
    } else if (cols.size > 1) {
      sys.error(s"Expected 1 column of type $colType but more than 1 found.")
    } else {
      cols(0)
    }
  }

  /**
   * Throws if the number of rows with given type is not 1
   */
  def getSingleRowByType(rowType: RowType): RowKey = {
    val rows = rowsByType(rowType)
    if (rows.size == 0) {
      sys.error(s"Expected 1 row of type $rowType but no rows of that type found.")
    } else if (rows.size > 1) {
      sys.error(s"Expected 1 row of type $rowType but more than 1 found. " + rows.tail.foldLeft(rows.head.toString)(_ + ", " + _))
    } else {
      rows(0)
    }
  }

  /**
   * Get cells from single column of colType for each row of rowType.
   *
   * Throws if there are multiple columns with ColType
   */
  def getSingleCol(colType: ColType, rowType: RowType): IndexedSeq[Cell] = {
    val colKey = getSingleColByType(colType)

    for (cellKey <- getCellKeys(colKey);
         rowKey = cellKey.rowKey;
         if rowTypes.contains(rowKey) && rowTypes(rowKey) == rowType) yield {
      getCell(cellKey)
    }
  }

  def getSingleRow(rowKey: RowKey, requiredColTypes: Set[ColType]): IndexedSeq[Cell] = {
    for (cellKey <- getCellKeys(rowKey);
         colKey = cellKey.colKey;
         if colTypes.contains(colKey) && requiredColTypes.contains(colTypes(colKey))) yield {
      getCell(cellKey)
    }
  }

  def getSingleRow(rowType: RowType, requiredColTypes: Set[ColType]): IndexedSeq[Cell] = {
    IndexedSeq() // getSingleRow(getSingleRowByType(rowType), requiredColTypes)
  }

  def getSingleCell(rowKey: RowKey, colType: ColType): Cell = {
    val colKey = getSingleColByType(colType)
    getCell(CellKey(rowKey, colKey))
  }


}
