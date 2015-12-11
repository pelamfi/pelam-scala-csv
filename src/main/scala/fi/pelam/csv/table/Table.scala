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
 * @constructor The actual constructor is on the companion object.
 *
 * @param cells All cells in a structure of nested `IndexedSeq`s. The order is first rows, then columns.
 * @param rowTypes A bidirectional map mapping rows to their row types and vice versa.
 *                 Multiple rows can have the same type.
 * @param colTypes A bidirectional map mapping columns to their column types and vice versa.
 *                 Multiple columns can have the same type.
 * @param metadata User extensible metadata that is piggybacked in the `Table` instance.
 *
 * @tparam RT The client specific row type.
 *
 * @tparam CT The client specific column type.
 *
 * @tparam M The type of the `metadata` parameter. Must be a sub type of [[TableMetadata]].
 *           This specifies the character set and separator to use when reading the CSV data from the input stream.
 */
final case class Table[RT, CT, M <: TableMetadata] private(
  cells: IndexedSeq[IndexedSeq[Cell]],
  rowTypes: SortedBiMap[RowKey, RT],
  colTypes: SortedBiMap[ColKey, CT],
  metadata: M) {
  import Table._

  type TableType = Table[RT, CT, M]

  /**
   * @param rowKey where new rows are added or old rows deleted
   * @param value for negative values, rows above `rowKey` are deleted
   *              For positive values rows at `rowKey` are added.
   * @return modified table
   */
  def resizeRows(rowKey: RowKey, value: Int, fillerGenerator: CellGenerator = emptyStringCell): TableType = {
    this
  }

  /**
   * @param colKey where new columns are added or old ones deleted
   * @param value for negative values, rows left of `colKey` are deleted.
   *              For positive values columns at `colKey` are added.
   * @return modified table
   */
  def resizeCols(colKey: ColKey, value: Int, fillerGenerator: CellGenerator = emptyStringCell): TableType = {
    this
  }

  /**
   * @param targetRegion defines a rectangular region of cells to be replaced. There can be gaps,
   *                    but the maximum and minimum row and column indices are used.
   *
   * @param replacementCells a rectangular region of cells to replace targetRegionw.
   *                         If there are gaps, fillerGenerator is used.
   *                         The replacementCells can define a smaller or larger
   *                         rectangular region.
   *                         If the region is smaller, overlapping rows and columns
   *                         are deleted from the "ends" of the target region.
   *
   * @param fillerGenerator When the `replacementCells` region is larger than `regionCells`
   *                        new rows and columns are added to the "ends" of the region
   *                        and the new cells in them are generated with this function.
   *                        This defaults to generating empty string cells.
   *
   * @return a new table with the replaced cells. Original table is never modified.
   */
  def updatedRegion(targetRegion: Region,
    replacementCells: Seq[Cell],
    fillerGenerator: CellGenerator = emptyStringCell): TableType = {
    val replacementRegion = spannedRegion(replacementCells)
    val resizedTable = resizeRows(targetRegion._2.rowKey, height(replacementRegion) - height(targetRegion), fillerGenerator)
    val resizedTable2 = resizedTable.resizeCols(targetRegion._2.colKey, width(replacementRegion) - width(targetRegion), fillerGenerator)
    resizedTable2.updatedCells(replacementCells)
  }

  def getRows(rowType: RT): IndexedSeq[IndexedSeq[Cell]] = ???

  /**
   * The vertical size of this table. The table has strict rectangular form.
   * Unused cells simply contain empty `StringCell`s.
   */
  val rowCount: Int = cells.size

  /**
   * The horizontal size of this table. The table has strict rectangular form.
   * Unused cells simply contain empty `StringCell`s.
   */
  val colCount: Int = cells.lift(0).map(_.size).getOrElse(0)

  // Verify table is strictly rectangular
  for (row <- cells) {
    require(row.size == colCount, s"Same number of columns required for reach row. ${row.size} vs ${colCount}")
  }

  /**
   * This method is a shorthand for `rowTypes.reverse`.
   * @return a map from row types to matching [[fi.pelam.csv.cell.RowKey RowKeys]] (row indices)
   */
  def rowsByType = rowTypes.reverse

  /**
   * This method is a shorthand for `colTypes.reverse`.
   * @return a map from column types to matching [[fi.pelam.csv.cell.ColKey ColKeys]] (column indices)
   */
  def colsByType = colTypes.reverse

  /**
   * Return a new table with given cells replacing the previous cells
   * at each the location `cell.cellKey`.
   */
  def updatedCells(newCells: TraversableOnce[Cell]): Table[RT, CT, M] = {

    var updatedCells = cells
    for ((rowIndex, rowCells) <- newCells.toTraversable.groupBy(_.rowIndex)) {
      checkRowInsideTable(rowIndex)
      var updatedRow = cells(rowIndex)
      for (cell <- rowCells) {
        val colIndex = cell.colIndex
        checkColInsideTable(colIndex)
        updatedRow = updatedRow.updated(colIndex, cell)
      }
      updatedCells = updatedCells.updated(rowIndex, updatedRow)
    }

    copy(cells = updatedCells)
  }

  /**
   * A convenient form for returning a new table with specified cells updated.
   * {{{
   *   val updated = table.updatedCells(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(1, 0), "bar"),
   * }}}
   */
  def updatedCells(cells: Cell*): Table[RT, CT, M] = updatedCells(cells)

  private[csv] def checkRowInsideTable(rowIndex: Int) = {
    if (rowIndex >= rowCount) {
      throw new IllegalArgumentException(s"Row number ${rowIndex + 1} " +
        s"is outside the number of rows $rowCount. Mark the row a comment?")
    }
  }

  private[csv] def checkColInsideTable(colIndex: Int) = {
    if (colIndex >= colCount) {
      throw new IllegalArgumentException(s"Column number ${colIndex + 1} " +
        s"is outside the number of columns $colCount. Mark the column a comment?")
    }
  }

  /**
   * Return a new table with given cell replacing the previous cell
   * in the location `cell.cellKey`.
   */
  def updatedCell(cell: Cell): Table[RT, CT, M] = {
    val rowIndex = cell.rowIndex
    val colIndex = cell.colIndex

    checkRowInsideTable(rowIndex)
    checkColInsideTable(colIndex)

    val updatedCells = cells.updated(rowIndex, cells(rowIndex).updated(colIndex, cell))

    copy(cells = updatedCells)
  }

  /**
   * Get all cells in a single sequence.
   * One way to think about this is that the rows are catenated one after the other.
   */
  def getCells(): IndexedSeq[Cell] = {
    for (i <- 0 until rowCount;
         j <- 0 until colCount) yield cells(i)(j)
  }

  /**
   * Get cell at specific row, column address.
   */
  def getCell(key: CellKey) = cells(key.rowIndex)(key.colIndex)

  /**
   * Get a cell from specified row matching the specified column type.
   * This method throws an error if more than 1 or zero cells match the criteria.
   * Example:
   * {{{
   *   // Get the user name cell from the 11th row.
   *   table.getSingleCell(RowKey(10), ColumnTypeUserName)
   * }}}
   *
   * @param rowKey identifies the row to target.
   * @param colType identifies the column type which must correspond to exactly 1 column.
   * @return the matching cell object.
   */
  def getSingleCell(rowKey: RowKey, colType: CT): Cell = {
    val colKey = getSingleColKeyByType(colType)
    getCell(CellKey(rowKey, colKey))
  }

  /**
   * Get all cells from the specified row.
   */
  def getCells(rowKey: RowKey): IndexedSeq[Cell] = {
    cells(rowKey.index)
  }

  /**
   * Get all cells from the specified column
   */
  def getCells(colKey: ColKey): IndexedSeq[Cell] = {
    for (i <- 0 until rowCount) yield cells(i)(colKey.index)
  }

  /**
   * Get cell keys corresponding to all cells on the specified row.
   */
  def getCellKeys(rowKey: RowKey): IndexedSeq[CellKey] = {
    for (i <- 0 until colCount) yield CellKey(rowKey, i)
  }

  /**
   * Get cell keys corresponding to all cells on the specified column.
   */
  def getCellKeys(colKey: ColKey): IndexedSeq[CellKey] = {
    for (i <- 0 until rowCount) yield CellKey(i, colKey)
  }

  /**
   * Gets a selected set of cells from a particular row.
   * The cells will be picked from columns having a column type (`CT`)
   * for which `colTypeMatcher` returns true.
   * Throws an exception if `RT` fits more than one or zero rows.
   * Here is an imaginary example: {{{
   *   // Gets the extra notes on projects and clients
   *   table.getSingleRow(ExtraNotesRow, Set(ProjectColumn, ClientColumn))
   * }}}
   */
  def getSingleRow(rowType: RT, colTypeMatcher: CT => Boolean): IndexedSeq[Cell] = {
    getSingleRow(getSingleRowKeyByType(rowType), colTypeMatcher)
  }

  /**
   * Gets a selected set of cells from a particular column.
   * The cells will be picked from rows having a row type (`RT`)
   * for which `rowTypeMatcher` returns true.
   * Throws an exception if `CT` fits more than one or zero columns.
   * Here is an imaginary example: {{{
   *   // Gets the street address for two types of addresses
   *   table.getSingleCol(Set(HomeAddressRow, BillingAddressRow), StreetAddressColumn)
   * }}}
   */
  def getSingleCol(rowTypeMatcher: RT => Boolean, colType: CT): IndexedSeq[Cell] = {
    getSingleCol(rowTypeMatcher, getSingleColKeyByType(colType))
  }

  /**
   * Otherwise same as `getSingleRow(RT, colTypeMatcher)`, but
   * a single bare column type is specified instead of the more general
   * predicate form.
   */
  def getSingleRow(rowType: RT, colType: CT): IndexedSeq[Cell] = {
    getSingleRow(getSingleRowKeyByType(rowType), (ct: CT) => ct == colType)
  }

  /**
   * Otherwise same as `getSingleCol(rowTypeMatcher, CT)`, but
   * a single bare row type is specified instead of the more general
   * predicate form.
   */
  def getSingleCol(rowType: RT, colType: CT): IndexedSeq[Cell] = {
    getSingleCol((rt: RT) => rt == rowType, getSingleColKeyByType(colType))
  }

  /**
   * Otherwise same as `getSingleRow(RT, colTypeMatcher)`, but
   * row is addressed directly with `RowKey` (row index) and
   * a single bare column type is specified instead of the more general
   * predicate form.
   */
  def getSingleRow(rowKey: RowKey, colType: CT): IndexedSeq[Cell] = {
    getSingleRow(rowKey, (ct: CT) => ct == colType)
  }

  /**
   * Otherwise same as `getSingleCol(rowTypeMatcher, CT)`, but
   * the column is addressed directly with `colKey` (column index) and
   * a single bare row type is specified instead of the more general
   * predicate form.
   */
  def getSingleCol(rowType: RT, colKey: ColKey): IndexedSeq[Cell] = {
    getSingleCol((rt: RT) => rt == rowType, colKey)
  }

  /**
   * Otherwise same as `getSingleRow(RT, colTypeMatcher)`, but
   * row is addressed directly with `RowKey` (row index).
   */
  def getSingleRow(rowKey: RowKey, colTypeMatcher: CT => Boolean): IndexedSeq[Cell] = {
    for (cellKey <- getCellKeys(rowKey);
         colKey = cellKey.colKey;
         if colTypes.contains(colKey) && colTypeMatcher(colTypes(colKey))) yield {
      getCell(cellKey)
    }
  }

  /**
   * Otherwise same as `getSingleCol(rowTypeMatcher, CT)`, but
   * the column is addressed directly with `colKey` (column index).
   */
  def getSingleCol(rowTypeMatcher: RT => Boolean, colKey: ColKey): IndexedSeq[Cell] = {
    for (cellKey <- getCellKeys(colKey);
         rowKey = cellKey.rowKey;
         if colTypes.contains(colKey) && rowTypeMatcher(rowTypes(rowKey))) yield {
      getCell(cellKey)
    }
  }

  /**
   * Find a single row with given type.
   * Throws if the number of rows with given type is not 1
   */
  def getSingleRowKeyByType(rowType: RT) = getSingleKeyByType(rowTypes.reverse, rowType, "row")

  /**
   * Find a single column with given type.
   * Throws if the number of columns with given type is not 1
   */
  def getSingleColKeyByType(colType: CT) = getSingleKeyByType(colTypes.reverse, colType, "column")

  override def toString() = {
    val builder = new StringBuilder()

    builder.append("columns:")

    for ((colKey, colType) <- colTypes) {
      builder.append(s"${colTypes(colKey)},")
    }

    builder.append("\n")

    for (row <- cells) {
      row.headOption.foreach { head =>
        val rowKey = head.cellKey.rowKey
        builder.append(s"${rowKey}/${rowTypes(rowKey)}:")
      }
      for (cell <- row) {
        cell match {
          case StringCell(_, s) => {
            builder.append(s)
          }
          case IntegerCell(_, v) => builder.append(s"i $v")
          case DoubleCell(_, v) => builder.append(s"d $v")
          case someCell => builder.append(s"${someCell.getClass().getSimpleName} ${someCell.value}")
        }
        builder.append(s",")
      }
      builder.append("\n")
    }
    builder.toString()
  }
}

object Table {

  /**
   * Defines a rectangular region. Both row and column index
   * of first `CellKey` must be smaller or equal to the indices
   * of the second `CellKey`.
   */
  type Region = (CellKey, CellKey)

  type CellGenerator = (CellKey) => Cell

  def emptyStringCell(cellKey: CellKey) = StringCell(cellKey, "")

  def width(region: Region) = region._2.colIndex - region._1.colIndex

  def height(region: Region) = region._2.rowIndex - region._1.rowIndex

  implicit def spannedRegion(cells: TraversableOnce[Cell]): Region = spannedRegionKeys(cells.map(_.cellKey))

  implicit def spannedRegionKeys(cellKeys: TraversableOnce[CellKey]): Region = {
    val initial = (Int.MaxValue, Int.MinValue, Int.MaxValue, Int.MinValue)

    val spannedIndices = cellKeys.foldLeft(initial)(
      (region, key) => (
        Math.min(region._1, Math.min(region._1, key.rowIndex)),
        Math.max(region._2, Math.max(region._2, key.rowIndex + 1)),
        Math.min(region._3, Math.min(region._3, key.colIndex)),
        Math.max(region._4, Math.max(region._4, key.colIndex + 1))
        ))

    if (spannedIndices == initial) {
      (CellKey(0, 0), CellKey(0, 0))
    } else {
      (CellKey(spannedIndices._1, spannedIndices._3), CellKey(spannedIndices._2, spannedIndices._4))
    }
  }

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
    metadata: M = SimpleMetadata()): Table[RT, CT, M] = {

    val maxRow = findKeyRangeEnd(rowTypes.keys)

    val maxCol = findKeyRangeEnd(colTypes.keys)

    val builtCells = buildCells(cells, maxRow, maxCol)

    Table(builtCells, rowTypes, colTypes, metadata)
  }

  // Helper method to find maximum key values from a sequence of cells.
  private[csv] def findKeyRangeEnd(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  // This is a helper method to convert a sequence of cells to a 2 dimensional IndexedSeq.
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
        val cell = initialCellMap.get(cellKey).getOrElse(emptyStringCell(cellKey))

        colArray(colIndex) = cell
      }
    }

    rowArray.map(_.toIndexedSeq).toIndexedSeq
  }

  // A helper method that throws if the number of columns/rows with given type is not 1.
  private[csv] def getSingleKeyByType[K <: AxisKey[K], T](keysByType: SortedMap[T, IndexedSeq[K]], colType: T, axisName: String): K = {
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
