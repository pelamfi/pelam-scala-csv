/*
 * This file is part of pelam-scala-csv
 *
 * Copyright © Peter Lamberg 2015 (pelam-scala-csv@pelam.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.pelam.csv.table

import fi.pelam.csv.cell._
import fi.pelam.csv.util.SortedBiMap

import scala.collection.SortedMap

/**
 * This class is part of the the higher level API for reading,
 * writing and processing CSV data.
 *
 * [[fi.pelam.csv.stream.CsvReader The simpler stream based API]] is enough for
 * many scenarios, but if several different sets of data will be pulled from
 * the same CSV file and the structure of the CSV file is not rigid, this API may
 * be a better fit.
 *
 * This class is an immutable container for [[fi.pelam.csv.cell.Cell Cells]] with optional
 * row and column types.
 *
 * Several methods are provided for getting cells based on row and column types.
 * For example
 *
 * == Example ==
 * This example constructs a table directly, although usually it is done via a
 * [[TableReader]]. `String` values are simply used for row and column types.
 *
 * {{{
 * val table = Table(
 *   List(StringCell(CellKey(0, 0), "name"),
 *     StringCell(CellKey(0, 1), "value"),
 *     StringCell(CellKey(1, 0), "foo"),
 *     IntegerCell(CellKey(1, 1), 1),
 *     StringCell(CellKey(2, 0), "bar"),
 *     IntegerCell(CellKey(2, 1), 2)
 *   ),
 *
 *   SortedBiMap(RowKey(0) -> "header",
 *     RowKey(1) -> "data",
 *     RowKey(2) -> "data"),
 *
 *   SortedBiMap(ColKey(0) -> "name",
 *     ColKey(1) -> "number")
 *  )
 *
 * table.getSingleCol("name", "data").map(_.value).toList
 * // Will give List("foo","bar")
 *
 * table.getSingleCol("number", "data").map(_.value).toList)
 * // Will give List(1,2)
 * }}}
 *
 * @constructor
 */
// TODO: ScalaDoc for the methods
case class Table[RT, CT, M <: TableMetadata] private(
  cells: IndexedSeq[IndexedSeq[Cell]],
  rowTypes: SortedBiMap[RowKey, RT],
  colTypes: SortedBiMap[ColKey, CT],
  metadata: M) {

  import Table._

  val rowCount: Int = cells.size

  val colCount: Int = cells.lift(0).map(_.size).getOrElse(0)

  for (row <- cells) {
    require(row.size == colCount, s"Same number of columns required for reach row. ${row.size} vs ${colCount}")
  }

  def updatedCells(cells: TraversableOnce[Cell]): Table[RT, CT, M] = {
    var table = this
    for (cell <- cells) {
      table = table.updatedCell(cell)
    }
    table
  }

  def updatedCells(cells: Cell*): Table[RT, CT, M] = updatedCells(cells)

  def updatedCell(cell: Cell): Table[RT, CT, M] = {
    val key = cell.cellKey

    if (key.rowIndex >= rowCount) {
      throw new IllegalArgumentException(s"Row number ${key.rowIndex + 1} " +
        s"is outside the number of rows $rowCount. Mark the row a comment?")
    }

    if (key.colIndex >= colCount) {
      throw new IllegalArgumentException(s"Column number ${key.colIndex + 1} " +
        s"is outside the number of columns $colCount. Mark the column a comment?")
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
  def getSingleColByType(colType: CT) = getSingleKeyByType(colTypes.reverse, colType, "column")

  /**
   * Throws if the number of rows with given type is not 1
   */
  def getSingleRowByType(rowType: RT) = getSingleKeyByType(rowTypes.reverse, rowType, "row")

  /**
   * Get cells from single column of colType for each row of rowType.
   *
   * Throws if there are multiple columns with CT
   */
  def getSingleCol(rowType: RT, colType: CT): IndexedSeq[Cell] = {
    val colKey = getSingleColByType(colType)

    for (cellKey <- getCellKeys(colKey);
         rowKey = cellKey.rowKey;
         if rowTypes.contains(rowKey) && rowTypes(rowKey) == rowType) yield {
      getCell(cellKey)
    }
  }

  def getSingleRow(rowKey: RowKey, requiredColTypes: Set[CT]): IndexedSeq[Cell] = {
    for (cellKey <- getCellKeys(rowKey);
         colKey = cellKey.colKey;
         if colTypes.contains(colKey) && requiredColTypes.contains(colTypes(colKey))) yield {
      getCell(cellKey)
    }
  }

  def getSingleRow(rowType: RT, requiredColTypes: Set[CT]): IndexedSeq[Cell] = {
    getSingleRow(getSingleRowByType(rowType), requiredColTypes)
  }

  def getSingleRow(rowKey: RowKey, colType: CT): IndexedSeq[Cell] = {
    getSingleRow(rowKey, Set[CT](colType))
  }

  def getSingleCell(rowKey: RowKey, colType: CT): Cell = {
    val colKey = getSingleColByType(colType)
    getCell(CellKey(rowKey, colKey))
  }
}

object Table {

  /**
   * This is the main constructor for table. Often this is not used directly, but through [[TableReader]].
   *
   * @param cells The cells to be used in the table in any order.
   * @param rowTypes a map that contains types for rows using the client code defined objects.
   * @param colTypes a map that contains types for columns using the client code defined objects.
   * @param metadata a user customizable metadata object than can piggyback additional information to this table object.
   * @tparam RT Client specified object type used for typing rows in CSV data.
   * @tparam CT Client specified object type used for typing columns in CSV data.
   * @return constructed Table object
   */
  def apply[RT, CT, M <: TableMetadata](
    cells: TraversableOnce[Cell] = IndexedSeq(),
    rowTypes: SortedBiMap[RowKey, RT] = SortedBiMap[RowKey, RT](),
    colTypes: SortedBiMap[ColKey, CT] = SortedBiMap[ColKey, CT](),
    metadata: M = SimpleTableMetadata()): Table[RT, CT, M] = {

    val maxRow = findKeyRangeEnd(rowTypes.keys)

    val maxCol = findKeyRangeEnd(colTypes.keys)

    val builtCells = buildCells(cells, maxRow, maxCol)

    Table(builtCells, rowTypes, colTypes, metadata)
  }

  def findKeyRangeEnd(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  /**
   * This is a helper method to convert a sequence of cells
   * to a 2 dimensional IndexedSeq.
   */
  private[csv] def buildCells(initialCells: TraversableOnce[Cell], rowCount: Int = 0, colCount: Int = 0): IndexedSeq[IndexedSeq[Cell]] = {

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
        val cell = initialCellMap.get(cellKey).getOrElse(new StringCell(cellKey, ""))

        colArray(colIndex) = cell
      }
    }

    rowArray.map(_.toIndexedSeq).toIndexedSeq
  }

  /**
   * Throws if the number of columns/rows with given type is not 1
   */
  def getSingleKeyByType[K <: AxisKey[K], T](keysByType: SortedMap[T, IndexedSeq[K]], colType: T, axisName: String): K = {
    val keys = keysByType(colType)
    if (keys.size == 0) {
      sys.error(s"Expected 1 $axisName of type $colType but no $axisName of that type found.")
    } else if (keys.size > 1) {
      sys.error(s"Expected 1 $axisName of type $colType but more than 1 found.")
    } else {
      keys(0)
    }
  }


}
