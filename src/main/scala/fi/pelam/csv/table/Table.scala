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
import fi.pelam.csv.table.TableUtil._
import fi.pelam.csv.util.SortedBiMap
import fi.pelam.csv.util.SortedBiMap._

import scala.collection.{SortedMap, SortedSet}

/**
 * This class is an immutable container for [[fi.pelam.csv.cell.Cell Cells]] with optional
 * row and column types. The ideas in the API roughly follow popular spread sheet programs.
 *
 * The cells are stored in rows which are numbered conceptually from top to bottom
 * and then in columns which are numbered from left to right.
 *
 * The row and column types are an additional abstraction with the purpose of simplifying
 * machine reading of complex spread sheets.
 *
 * This class is part of the the higher level API for reading,
 * writing and processing CSV data.
 *
 * [[fi.pelam.csv.stream.CsvReader The simpler stream based API]] is enough for
 * many scenarios, but if several different sets of data will be pulled from
 * the same CSV file and the structure of the CSV file is not rigid, this API may
 * be a better fit.
 *
 * Several methods are provided for getting cells based on the row and column types.
 * For example
 *
 * == Example ==
 * This example constructs a table directly, although usually it is done via a
 * [[TableReader]]. In this example, simple `String` values are used for row
 * and column types, although usually an enumeration or
 * case object type solution is cleaner and safer.
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
 * == Note on row and column numbers ==
 *
 * Internally rows and columns have zero based index numbers, but in some cases
 * like in `toString` methods of `Cell` and `CellKey` the index numbers are represented similarly
 * to popular spread sheet programs. In that csae row numbers are one based and column
 * numbers are alphabetic.

 * @constructor The actual constructor is on the companion object.
 *
 * @param cells All cells in a structure of nested `IndexedSeq`s. The order is first rows, then columns.
 *
 * @param rowTypes A bidirectional map mapping rows to their row types and vice versa.
 *                 Multiple rows can have the same type.
 *
 * @param colTypes A bidirectional map mapping columns to their column types and vice versa.
 *                 Multiple columns can have the same type.
 *
 * @param metadata User extensible metadata that is piggybacked in the `Table` instance.
 *
 * @tparam RT The client specific row type.
 *
 * @tparam CT The client specific column type.
 *
 * @tparam M The type of the `metadata` parameter. Must be a sub type of [[TableMetadata]].
 *           This specifies the character set and separator to use when reading the CSV data from the input stream.
 */
// TODO: Reorder methods
final case class Table[RT, CT, M <: TableMetadata](
  cells: IndexedSeq[IndexedSeq[Cell]],
  rowTypes: SortedBiMap[RowKey, RT],
  colTypes: SortedBiMap[ColKey, CT],
  metadata: M) {
  import Table._

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

  require(rowTypes.keys.foldLeft(true)((acc, rowKey) => acc && rowKey.inRange(rowCount)),
    "Row type map keys must match rows in the table")

  require(colTypes.keys.foldLeft(true)((acc, colKey) => acc && colKey.inRange(colCount)),
    "Column type map keys must match columns in the table")

  // Verify table is strictly rectangular and that each cell's key matches the position in table
  for ((row, rowIndex) <- cells.zipWithIndex) {
    require(row.size == colCount, s"Same number of columns required for reach row. ${row.size} vs ${colCount}")
    for ((cell, colIndex) <- row.zipWithIndex) {
      require(cell.cellKey.rowIndex == rowIndex &&
        cell.cellKey.colIndex == colIndex,
        s"Cell $cell should be in that position in the datastructure, \n but it is at position ${CellKey(rowIndex, colIndex)}.")
    }
  }

  type TableType = Table[RT, CT, M]

  def getRow(cellKey: CellKey) = cells(cellKey.rowIndex)

  /**
   * @param rowKey where new rows are added or old rows deleted.
   *               If rows are deleted this will be the first row to go and further rows will be the
   *               ones before this. If rows are added, they are added after this row with the same
   *               type as this row.
   * @param value for negative values, rows above `rowKey` are deleted
   *              For positive values rows at `rowKey` are added.
   *              For zero, this table is returned.
   * @return modified table
   */
  def resizeRows(rowKey: RowKey, value: Int, fillerGenerator: CellGenerator = emptyStringCell): TableType = value match {
    case value if value < 0 => {
      val keepFromStart = rowKey.index + value + 1
      val dropAndKeepFromEnd = rowKey.index + 1
      val beforeRemoved = cells.take(keepFromStart)
      val rowsRemoved = beforeRemoved ++ renumberRows(beforeRemoved.size, cells.drop(dropAndKeepFromEnd))

      val rowTypesRemoved = deleteTypeMapSlice(keepFromStart, dropAndKeepFromEnd, rowTypes)

      new Table[RT, CT, M](rowsRemoved, rowTypesRemoved, colTypes, metadata)
    }

    case value if value > 0 => {
      val newIndices = rowKey.index + 1 until rowKey.index + value + 1

      val newRows = for (rowIndex <- newIndices) yield {
        for (colIndex <- 0 until colCount) yield {
          fillerGenerator(CellKey(rowIndex, colIndex))
        }
      }

      val keepFromStart = rowKey.index + 1
      val dropAndKeepFromEnd = rowKey.index + 1
      val indicesOk = cells.take(keepFromStart) ++ newRows
      val rowsAdded = indicesOk ++ renumberRows(indicesOk.size, cells.drop(dropAndKeepFromEnd))

      val rowTypesAdded = addTypeMapSlice(rowKey, newIndices.size, rowTypes)

      new Table[RT, CT, M](rowsAdded, rowTypesAdded, colTypes, metadata)
    }

    case _ => this
  }

  /**
   * @param colKey where new columns are added or old ones deleted
   *               If columns are deleted this will be the first columns to go and further columns will be the
   *               ones before this. If columns are added, they are added before this column with the same
   *               type as this column.
   * @param value for negative values, rows left of `colKey` are deleted.
   *              For positive values columns at `colKey` are added.
   *              For zero, this table is returned.
   * @param updateSide Allows adding the new colums _after_ the column indicated by `colKey`.
   * @return modified table
   */
  // TODO: Direction should be in resizeRows and also apply to deleting rows
  def resizeCols(colKey: ColKey,
    value: Int,
    fillerGenerator: CellGenerator = emptyStringCell,
    updateSide: HorizontalDirection = LeftColumn): TableType = value match {
    case value if value < 0 => {
      val keepFromStart = colKey.index + value + 1
      val dropAndKeepFromEnd = colKey.index + 1

      // extract
      val colsRemoved = for (row <- cells) yield {
        val endRenumbered = for ((cell, offset) <- row.drop(dropAndKeepFromEnd).zipWithIndex;
                                 index = keepFromStart + offset) yield {
          cell.updatedCellKey(CellKey(cell.rowIndex, index))
        }
        row.take(keepFromStart) ++ endRenumbered
      }

      val colTypesRemoved = deleteTypeMapSlice(keepFromStart, dropAndKeepFromEnd, colTypes)

      new Table[RT, CT, M](colsRemoved, rowTypes, colTypesRemoved, metadata)
    }

    case value if value > 0 => {
      val offset = updateSide match {
        case LeftColumn => 0
        case RightColumn => 1
      }

      val newIndices = colKey.index + offset until colKey.index + value + offset
      val keepFromStart = colKey.index + offset
      val dropAndKeepFromEnd = colKey.index + offset

      val colsAdded = for ((row, rowIndex) <- cells.zipWithIndex) yield {
        val newCols = for (colIndex <- newIndices) yield {
          fillerGenerator(CellKey(rowIndex, colIndex))
        }

        val tailRenumbered = for (cell <- row.drop(dropAndKeepFromEnd)) yield {
          cell.updatedCellKey(cell.cellKey.withColOffset(newIndices.size))
        }

        row.take(keepFromStart) ++ newCols ++ tailRenumbered
      }

      val colTypesAdded = addTypeMapSlice(colKey, newIndices.size, colTypes)

      new Table[RT, CT, M](colsAdded, rowTypes, colTypesAdded, metadata)
    }

    case _ => this
  }

  /**
   * @param targetRegion defines a rectangular region of cells to be replaced. Region
   *                     spanned by `replacementCells` does not need to fit targetRegion.
   *                     Table will be resized to match. See below for more details.
   *
   * @param replacementCells a rectangular region of cells to replace `targetRegion`.
   *                         Gaps are ok.
   *                         The `replacementCells` can define (span) a different
   *                         rectangular region than the targetRegion.
   *
   *                         See [[.resizeRegion]] for details on how the resizing works.
   *
   * @param fillerGenerator When new cells need to be created, this is used.
   *
   * @return a new table with the replaced cells. Original table is not modified.
   */
  def updatedRegion(targetRegion: Region,
    replacementCells: TraversableOnce[Cell],
    fillerGenerator: CellGenerator = emptyStringCell): TableType = {

    val traversableReplacement = replacementCells.toTraversable

    val replacementRegion = spannedRegion(traversableReplacement)

    val resizedTable = resized(targetRegion, replacementRegion, fillerGenerator)

    resizedTable.updatedCells(traversableReplacement)
  }

  /**
    * Otherwise same as `updatedRegion`, but `replacementCells` are renumbered
    * from left to right and top to bottom to
    * tightly fill the `targetRegion`. If the cell counts don't match,
   * `targetRegion` is shrinked or grown to fit all `replacementCells`
   */
  def updatedRows(targetRegion: Region,
    replacementCells: TraversableOnce[Cell],
    fillerGenerator: CellGenerator = emptyStringCell): TableType = {

    val renumbered = renumberAsRows(replacementCells, targetRegion)

    updatedRegion(targetRegion, renumbered, fillerGenerator)
  }

  /**
    * Otherwise same as `updatedRegion`, but `replacementCells` are renumbered
    * from top to bottom and left to right
    * tightly fill the `targetRegion`. If the cell counts don't match,
    * `targetRegion` is shrinked or grown to fit all `replacementCells`
    */
  def updatedColumns(targetRegion: Region,
    replacementCells: TraversableOnce[Cell],
    fillerGenerator: CellGenerator = emptyStringCell): TableType = {

    val renumbered = renumberAsColumns(replacementCells, targetRegion)

    updatedRegion(targetRegion, renumbered, fillerGenerator)
  }

  /**
   * A region based resize method for table that can grow or shrink a region
   * as needed.
   *
   * @param targetRegion defines a rectangular region of cells to be replaced. Region
   *                     spanned by `replacementCells` does not need to fit targetRegion.
   *                     Table will be resized to match. See below for more details.
   *
   * @param resizedRegion a rectangular region to replace `targetRegion`.
   *
   *                      The idea is that `resizedRegion` can define
   *                      (span) a different rectangular region than the
   *                      `targetRegion`.
   *
   *                      If there are extra cells in the `targetRegion`, rows and columns
   *                      are deleted from the "ends" of the `targetRegion`.
   *
   *                      If the `resizedCells` doesn't fit in `targetRegion`, `targetRegion`
   *                      is expanded to contain it. New rows and columns are generated
   *                      with `fillerGenerator` as needed.
   *
   *                      There is one limitation however. the top left corner of
   *                      `resizedRegion` must be equal to or towards down and right
   *                      with respect to the top left corner of `targetRegion`.
   *
   *
   * @param fillerGenerator When new cells need to be created, this is used.
   */
  def resized(targetRegion: (CellKey, CellKey), resizedRegion: (CellKey, CellKey), fillerGenerator: CellGenerator): TableType = {
    val topLeftMinRegion = topLeftMin(resizedRegion, targetRegion)
    val heightResize: Int = height(topLeftMinRegion) - height(targetRegion)
    val widthResize: Int = width(topLeftMinRegion) - width(targetRegion)
    val resizeCellKey = CellKey(targetRegion._2.rowKey.withOffset(-1), targetRegion._2.colKey.withOffset(-1))
    val resizedTable = resized(resizeCellKey, heightResize, widthResize, fillerGenerator)
    resizedTable
  }

  def resized(resizeCellKey: CellKey, rowsResize: Int, colsResize: Int, fillerGenerator: CellGenerator): TableType = {
    val resizedTable = resizeRows(resizeCellKey.rowKey, rowsResize, fillerGenerator)
    val resizedTable2 = resizedTable.resizeCols(resizeCellKey.colKey, colsResize, fillerGenerator, updateSide = RightColumn)
    resizedTable2
  }

  /**
   * This method is a shorthand for `rowTypes.reverse`.
   * @return a map from row types to matching [[fi.pelam.csv.cell.RowKey RowKeys]] (row indices)
   */
  def rowsByType: SortedMap[RT, IndexedSeq[RowKey]] = rowTypes.reverse

  /**
   * This method is a shorthand for `colTypes.reverse`.
   * @return a map from column types to matching [[fi.pelam.csv.cell.ColKey ColKeys]] (column indices)
   */
  def colsByType: SortedMap[CT, IndexedSeq[ColKey]] = colTypes.reverse

  /**
   * Return a new table with given cells replacing the previous cells
   * at each the location `cell.cellKey`.
   */
  def updatedCells(newCells: TraversableOnce[Cell]): TableType = {

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
  def updatedCells(cells: Cell*): TableType = updatedCells(cells)

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
  def updatedCell(cell: Cell): TableType = {
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
   * Get a full rows from table defined by `rowType`.
   */
  def getRows(rowType: RT): IndexedSeq[IndexedSeq[Cell]] = {
    val rowKeys = rowTypes.reverse(rowType)
    for (rowKey <- rowKeys) yield {
      cells(rowKey.index)
    }
  }

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
         if rowTypes.contains(rowKey) && rowTypeMatcher(rowTypes(rowKey))) yield {
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

    for (colKey <- colKeys) {
      builder.append(s"${colKey.index}/${colTypes.get(colKey).map(_.toString).getOrElse("")},")
    }

    builder.append("\n")

    builder.append(rowsToString(cells, rowKey => s"${rowKey.index}/${rowTypes.get(rowKey).map(_.toString).getOrElse("")}:"))

    builder.toString()
  }

  /**
    * Table can be "projected" ie. select some rows and columns and create a new table.
    *
    * See [[TableProjection]] for an example.
    */
  def projection = new TableProjection(this)

  /**
    * Same as `projection`, but start with all rows and columns which can
    * then be removed.
    *
    * See [[TableProjection]] for more details.
    */
  def projectionFull = new TableProjection(this, rowKeys, colKeys)

  val rowKeys: SortedSet[RowKey] = SortedSet[RowKey]() ++ (0 until rowCount).map(RowKey(_))

  val colKeys: SortedSet[ColKey] = SortedSet[ColKey]() ++ (0 until colCount).map(ColKey(_))
}

object Table {

  /**
   * Defines a rectangular region. Both row and column index
   * of first `CellKey` must be smaller or equal to the indices
   * of the second `CellKey`.
   *
   * Region is defined as an half open range ie CellKey 2 is not
   * included in the region.
   */
  type Region = (CellKey, CellKey)

  type CellGenerator = (CellKey) => Cell

  val emptyRegion = (CellKey(0, 0), CellKey(0, 0))

  implicit def spannedRegion(cells: TraversableOnce[Cell]): Region = spannedRegionKeys(cells.map(_.cellKey))

  implicit def toStringCells(strings: TraversableOnce[String]): TraversableOnce[Cell] = {
    strings.map(StringCell(CellKey.invalid, _))
  }

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
      emptyRegion
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

  def rowsToString(rows: IndexedSeq[IndexedSeq[Cell]], rowHeader: (RowKey) => String = rowKey => s"$rowKey:"): String = {
    val builder = new StringBuilder()
    for (row <- rows) {
      row.headOption.foreach { head =>
        val rowKey = head.cellKey.rowKey
        builder.append(rowHeader(rowKey))
      }
      for (cell <- row) {
        builder.append(cell.shortString())
        builder.append(s",")
      }
      builder.append("\n")
    }

    builder.toString()
  }

  sealed trait HorizontalDirection

  case object LeftColumn extends HorizontalDirection

  case object RightColumn extends HorizontalDirection

}
