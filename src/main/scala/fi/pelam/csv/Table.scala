package fi.pelam.csv

import java.nio.charset.Charset
import java.util.Locale

import fi.pelam.ahma.serialization.{ColType, RowType}

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

object Table {
  val rowTypeCol = ColKey(0)

  // http://stackoverflow.com/a/24222250/1148030
  def reverseMap[A, B](map: scala.collection.Map[A, B]): Map[B, IndexedSeq[A]] = map.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq)

  def reverseMapSorted[A, B <: Ordered[B]](map: Map[A, B]): SortedMap[B, IndexedSeq[A]] =
    TreeMap[B, IndexedSeq[A]]() ++ reverseMap(map)

  def buildCells(initialCells: TraversableOnce[Cell], rowCount: Int, colCount: Int): IndexedSeq[IndexedSeq[Cell]] = {
    val initialCellMap = initialCells.map(cell => cell.cellKey -> cell).toMap

    val rowArray = new Array[Array[Cell]](rowCount)

    for (rowIndex <- 0 until rowCount) {

      val rowKey = RowKey(rowIndex)
      val colArray = new Array[Cell](colCount)

      rowArray(rowIndex) = colArray

      for (colIndex <- 0 until colCount) {

        val cellKey = CellKey(rowKey, colIndex)
        val cell = initialCellMap.get(cellKey).getOrElse(new StringCell(cellKey, ""))

        colArray(colIndex) = cell
      }
    }

    rowArray.map(_.toIndexedSeq).toIndexedSeq
  }

  def tableSize(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  def apply(charset: Charset,
    csvSeparator: Char,
    stringLocale: Locale,
    dataLocale: Locale,
    rowTypes: SortedMap[RowKey, RowType],
    colTypes: SortedMap[ColKey, ColType],
    cells: TraversableOnce[Cell]): Table = {

    val builtCells = buildCells(cells, tableSize(rowTypes.keys), tableSize(colTypes.keys))

    Table(charset, csvSeparator, stringLocale, dataLocale, rowTypes, colTypes, builtCells)
  }

}

case class Table(charset: Charset,
  csvSeparator: Char,
  stringLocale: Locale,
  dataLocale: Locale,
  rowTypes: SortedMap[RowKey, RowType],
  colTypes: SortedMap[ColKey, ColType],
  cells: IndexedSeq[IndexedSeq[Cell]]) {

  import Table._

  val rowsByType: Map[RowType, IndexedSeq[RowKey]] = reverseMap(rowTypes)

  val colsByType: Map[ColType, IndexedSeq[ColKey]] = reverseMap(colTypes)

  val rowCount: Int = rowTypes.keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  val colCount: Int = colTypes.keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  def updatedCells(cells: TraversableOnce[Cell]): Table = {
    var table = this
    for (cell <- cells) {
      table = table.updatedCell(cell)
    }
    table
  }

  def updatedCells(cells: Cell*): Table = updatedCells(cells)

  def updatedCell(cell: Cell): Table = {
    val key = cell.cellKey

    if (key.rowIndex >= rowCount) {
      throw new IllegalArgumentException(s"Row number ${key.rowIndex + 1} is outside the number of rows $rowCount. Mark the row a comment?")
    }

    if (key.colIndex >= colCount) {
      throw new IllegalArgumentException(s"Column number ${key.colIndex + 1} is outside the number of columns $colCount. Mark the column a comment?")
    }

    val updatedCells = cells.updated(key.rowIndex, cells(key.rowIndex).updated(key.colIndex, cell))

    copy(cells = updatedCells)
  }

  def getCell(key: CellKey) = cells(key.rowIndex)(key.colIndex)

  def getCells(rowKey: RowKey): IndexedSeq[Cell] = {
    for (i <- 0 until colCount) yield cells(rowKey.index)(i)
  }

  def getCells(): IndexedSeq[Cell] = {
    for (i <- 0 until rowCount;
         j <- 0 until colCount) yield cells(i)(j)
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
    getSingleRow(getSingleRowByType(rowType), requiredColTypes)
  }

  def getSingleRow(rowKey: RowKey, colType: ColType): IndexedSeq[Cell] = {
    getSingleRow(rowKey, Set[ColType](colType))
  }

  def getSingleCell(rowKey: RowKey, colType: ColType): Cell = {
    val colKey = getSingleColByType(colType)
    getCell(CellKey(rowKey, colKey))
  }


}
