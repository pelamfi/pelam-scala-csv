package fi.pelam.csv.table

import fi.pelam.csv.cell._
import fi.pelam.csv.util.SortedBiMap

import scala.collection.SortedMap
import scala.collection.generic.CanBuildFrom

/**
 * Collection of helper methods for [[Table]] and [[TableProjection]] implementation.
 */
object TableUtil {

  /**
   * Defines a rectangular region. Both row and column index
   * of first `CellKey` must be smaller or equal to the indices
   * of the second `CellKey`.
   *
   * Region is defined as an half open range ie CellKey 2 is not
   * included in the region.
   */
  type Region = (CellKey, CellKey)

  def emptyStringCell(cellKey: CellKey) = StringCell(cellKey, "")

  def width(region: Region) = region._2.colIndex - region._1.colIndex

  def height(region: Region) = region._2.rowIndex - region._1.rowIndex

  /**
   * Let `a` define bottom right corner, but get top left corner
   * as a minimum of coordinates from top left corners of `a` and `b`.
   */
  def topLeftMin(a: Region, b: Region): Region = (
    CellKey(Math.min(a._1.rowIndex, b._1.rowIndex),
      Math.min(a._1.colIndex, b._1.colIndex)),
    a._2)

  // Helper method to find maximum key values from a sequence of cells.
  private[table] def findKeyRangeEnd(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  // This is a helper method to convert a sequence of cells to a 2 dimensional IndexedSeq.
  private[table] def buildCells(initialCells: TraversableOnce[Cell], rowCount: Int = 0, colCount: Int = 0): IndexedSeq[IndexedSeq[Cell]] = {

    val initialCellMap = initialCells.map(cell => cell.cellKey -> cell).toMap

    // Row count is maximum from argument and largest row number seen in cells
    val finalRowCount = initialCellMap.keys.foldLeft(rowCount)((a, b) => scala.math.max(a, b.rowKey.index + 1))

    // Column count is maximum from argument and largest column number seen in cells
    val finalColCount = initialCellMap.keys.foldLeft(colCount)((a, b) => scala.math.max(a, b.colKey.index + 1))

    val rowArray = new Array[Array[Cell]](finalRowCount)

    for (rowIndex <- 0 until finalRowCount) {

      val rowKey = RowKey(rowIndex)
      val colArray = new Array[Cell](finalColCount)

      rowArray(rowIndex) = colArray

      for (colIndex <- 0 until finalColCount) {

        val cellKey = CellKey(rowKey, colIndex)
        val cell = initialCellMap.get(cellKey).getOrElse(emptyStringCell(cellKey))

        colArray(colIndex) = cell
      }
    }

    rowArray.map(_.toIndexedSeq).toIndexedSeq
  }

  private[table] def getSingleKeyByType[K <: AxisKey[K], T](keysByType: SortedMap[T, IndexedSeq[K]], colType: T, axisName: String): K = {
    val keys = keysByType(colType)
    if (keys.size == 0) {
      sys.error(s"Expected 1 $axisName of type $colType but no $axisName of that type found.")
    } else if (keys.size > 1) {
      sys.error(s"Expected 1 $axisName of type $colType but more than 1 found.")
    } else {
      keys(0)
    }
  }

  private[table] def renumberTypeMap[K <: AxisKey[K], T](firstIndex: Int, typeMap: SortedBiMap[K, T])(implicit builder: CanBuildFrom[SortedBiMap[K, T], (K, T), SortedBiMap[K, T]]): SortedBiMap[K, T] = {
    val b = builder()
    var offset = 0
    for ((axisKey, rowType) <- typeMap) {
      val index = firstIndex + offset
      b += ((axisKey.updated(index), rowType))
      offset += 1
    }
    b.result()
  }

  private[table] def renumberTypeMapByMap[K <: AxisKey[K], T](typeMap: SortedBiMap[K, T], keyMap: K => K)(implicit builder: CanBuildFrom[SortedBiMap[K, T], (K, T), SortedBiMap[K, T]]): SortedBiMap[K, T] = {
    val b = builder()
    for ((axisKey, rowType) <- typeMap) {
      b += ((keyMap(axisKey), rowType))
    }
    b.result()
  }

  def deleteTypeMapSlice[K <: AxisKey[K], T](start: Int, end: Int, typeMap: SortedBiMap[K, T])(implicit builder: CanBuildFrom[SortedBiMap[K, T], (K, T), SortedBiMap[K, T]]): SortedBiMap[K, T] = {
    val b = builder()
    val size = end - start
    for (pair <- typeMap) {
      val index = pair._1.index
      if (index < start) {
        b += pair
      } else if (index >= end) {
        b += ((pair._1.withOffset(-size), pair._2))
      }
    }
    b.result()
  }

  def addTypeMapSlice[K <: AxisKey[K], T](start: K, count: Int, typeMap: SortedBiMap[K, T])(implicit builder: CanBuildFrom[SortedBiMap[K, T], (K, T), SortedBiMap[K, T]]): SortedBiMap[K, T] = {
    val b = builder()
    for (pair <- typeMap) {
      val index = pair._1.index
      if (index < start.index) {
        b += pair
      } else {
        b += ((pair._1.withOffset(count), pair._2))
      }
    }
    val fill = typeMap.get(start)
    for (fill <- fill) {
      for (i <- 0 until count) {
        b += ((start.withOffset(i), fill))
      }
    }
    b.result()
  }

  private[table] def renumberRows(firstIndex: Int, cells: IndexedSeq[IndexedSeq[Cell]]): IndexedSeq[IndexedSeq[Cell]] = {
    for ((row, offset) <- cells.zipWithIndex;
         index = firstIndex + offset) yield {
      for (cell <- row) yield {
        cell.updatedCellKey(CellKey(index, cell.colIndex))
      }
    }
  }

  /**
   * Renumber `cells` to fit into `targetRegion` going from top left to right
   * and then down row by row.
   *
   * If there are more `cells` than can fit in `targetRegion` then the
   * numbering continues "below" `targetRegion`.
   */
  private[table] def renumberedAsRows(cells: TraversableOnce[Cell], targetRegion: Region): TraversableOnce[Cell] = {
    val top = targetRegion._1.rowIndex
    val left = targetRegion._1.colIndex
    var rowIndex = top
    var colIndex = left
    val regionWidth = width(targetRegion)
    val rightMax = left + regionWidth - 1
    for (cell <- cells) yield {
      val renumbered = cell.updatedCellKey(CellKey(rowIndex, colIndex))
      if (colIndex == rightMax) {
        rowIndex += 1
        colIndex = left
      } else {
        colIndex += 1
      }
      renumbered
    }
  }

  /**
    * Renumber `cells` to fit into `targetRegion` going from top left down
    * and then right column by column.
    *
    * If there are more `cells` than can fit in `targetRegion` then the
    * numbering continues right of `targetRegion`.
    */
  private[table] def renumberedAsCols(cells: TraversableOnce[Cell], targetRegion: Region): TraversableOnce[Cell] = {
    val top = targetRegion._1.rowIndex
    val left = targetRegion._1.colIndex
    var rowIndex = top
    var colIndex = left
    val regionHeight = height(targetRegion)
    val bottomMax = top + regionHeight - 1
    for (cell <- cells) yield {
      val renumbered = cell.updatedCellKey(CellKey(rowIndex, colIndex))
      if (rowIndex == bottomMax) {
        colIndex += 1
        rowIndex = top
      } else {
        rowIndex += 1
      }
      renumbered
    }
  }

  private[table] def axisKeyRenumberingMap[K <: AxisKey[K]](rowsSeq: TraversableOnce[K]): Map[K, K] = {
    val rowsToIndexBuilder = Map.newBuilder[K, K]

    var rowIndex = 0
    for (rowKey <- rowsSeq) {
      rowsToIndexBuilder += ((rowKey, rowKey.updated(rowIndex)))
      rowIndex += 1
    }

    rowsToIndexBuilder.result()
  }

}
