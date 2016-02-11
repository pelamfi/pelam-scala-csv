package fi.pelam.csv.table

import fi.pelam.csv.cell._
import fi.pelam.csv.table.TableUtil._
import fi.pelam.csv.util.SortedBiMap._

import scala.collection.{SortedMap, SortedSet}

/**
  * Part of the API to "project" a [[Table]]. Idea is to pick rows and columns
  * in an fluent and immutable way, and then get a table with just the selected
  * rows and columns. This is useful for example just displaying or logging certain
  * data.
  *
  * Example:
  * {{{
  *   import TableProjection._ // Import implicit toTable and toProjection
  *
  *   val table: Table = ...
  *   println(table.withColTypes(Name, Price).withRowTypes(Item))
  *
  *   // The inverse may also be useful for removing some data
  *
  *   println(table.withColTypes(Comments).inverse)
  * }}}
  */
case class TableProjection[RT, CT, M <: TableMetadata](
  baseTable: Table[RT, CT, M],
  rows: SortedSet[RowKey] = SortedSet(),
  cols: SortedSet[ColKey] = SortedSet()) {

  import TableProjection._

  type TableType = Table[RT, CT, M]
  type Projection = TableProjection[RT, CT, M]

  def inverse: Projection = copy(rows = baseTable.rowKeys -- rows, cols = baseTable.colKeys -- cols)

  def all: Projection = copy(cols = baseTable.colKeys, rows = baseTable.rowKeys)

  def allCols: Projection = copy(cols = baseTable.colKeys)

  def allRows: Projection = copy(rows = baseTable.rowKeys)

  def withRowTypes(rowTypes: RT*): Projection = {
    copy(rows = addByType[RT, RowKey](rows, rowTypes, baseTable.rowsByType))
  }

  def withRowTypes(rowTypes: TraversableOnce[RT]): Projection = {
    copy(rows = addByType[RT, RowKey](rows, rowTypes, baseTable.rowsByType))
  }

  def withColTypes(colTypes: CT*): Projection = {
    copy(cols = addByType[CT, ColKey](cols, colTypes, baseTable.colsByType))
  }

  def withColTypes(colTypes: TraversableOnce[CT]): Projection = {
    copy(cols = addByType[CT, ColKey](cols, colTypes, baseTable.colsByType))
  }

  def withoutRowTypes(rowTypes: RT*): Projection = {
    copy(rows = removeByType[RT, RowKey](rows, rowTypes, baseTable.rowsByType))
  }

  def withoutRowTypes(rowTypes: TraversableOnce[RT]): Projection = {
    copy(rows = removeByType[RT, RowKey](rows, rowTypes, baseTable.rowsByType))
  }

  def withoutColTypes(colTypes: CT*): Projection = {
    copy(cols = removeByType[CT, ColKey](cols, colTypes, baseTable.colsByType))
  }

  def withoutColTypes(colTypes: TraversableOnce[CT]): Projection = {
    copy(cols = removeByType[CT, ColKey](cols, colTypes, baseTable.colsByType))
  }

  def rows(transform: SortedSet[RowKey] => TraversableOnce[RowKey]): Projection = {
    val modifiedRows = SortedSet[RowKey]() ++ transform(rows)

    require(modifiedRows.foldLeft(true)((acc, rowKey) => acc && rowKey.inRange(baseTable.rowCount)),
      "Row keys must match rows in the table")

    copy(rows = modifiedRows)
  }

  def cols(transform: SortedSet[ColKey] => TraversableOnce[ColKey]): Projection = {
    val modifiedCols = SortedSet[ColKey]() ++ transform(cols)

    require(modifiedCols.foldLeft(true)((acc, colKey) => acc && colKey.inRange(baseTable.colCount)),
      "Column keys must match colmns in the table")

    copy(cols = modifiedCols)
  }

  /**
   * Construct a copy of the table with only selected a subset of rows and columns.
   */
  lazy val projected: TableType = {
    val cells = baseTable.cells
    val rowTypes = baseTable.rowTypes
    val colTypes = baseTable.colTypes

    var rowIndex = 0
    var colIndex = 0

    val rowsSeq = rows.toIndexedSeq
    val colsSeq = cols.toIndexedSeq

    val projectedCells: IndexedSeq[IndexedSeq[Cell]] = for (rowKey <- rowsSeq) yield {
      colIndex = 0
      val oldRow = cells(rowKey.index)
      val row = for (colKey <- colsSeq) yield {
        val cell = oldRow(colKey.index).updatedCellKey(CellKey(rowIndex, colIndex))
        colIndex += 1
        cell
      }
      rowIndex += 1
      row
    }

    val rowsToIndex: Map[RowKey, RowKey] = axisKeyRenumberingMap(rowsSeq)

    val colsToIndex: Map[ColKey, ColKey] = axisKeyRenumberingMap(colsSeq)

    val projectedRowTypes = renumberTypeMapByMap(rowTypes.filterKeys(rows.contains(_)), rowsToIndex)

    val projectedColTypes = renumberTypeMapByMap(colTypes.filterKeys(cols.contains(_)), colsToIndex)

    Table(projectedCells, projectedRowTypes, projectedColTypes, baseTable.metadata)
  }

  def spannedRegion: Table.Region = {
    if (rows.isEmpty || cols.isEmpty) {
      Table.emptyRegion
    } else {
      (CellKey(rows.head, cols.head), CellKey(rows.last.withOffset(1), cols.last.withOffset(1)))
    }
  }

  override def toString = s"TableProjection($rows, $cols)"
}

object TableProjection {
  implicit def toProjection[RT, CT, M <: TableMetadata](table: Table[RT, CT, M]): TableProjection[RT, CT, M] = table.projection

  implicit def toTable[RT, CT, M <: TableMetadata](projection: TableProjection[RT, CT, M]): Table[RT, CT, M] = projection.projected

  private def addByType[T, K <: AxisKey[K]](prev: Traversable[K],
    types: TraversableOnce[T],
    axisByType: SortedMap[T, IndexedSeq[K]]): SortedSet[K] = {
    val builder = SortedSet.newBuilder[K]
    builder ++= prev
    for (t <- types) {
      builder ++= axisByType(t)
    }
    builder.result
  }

  private def removeByType[T, K <: AxisKey[K]](prev: SortedSet[K],
    types: TraversableOnce[T],
    axisByType: SortedMap[T, IndexedSeq[K]]): SortedSet[K] = {
    val builder = SortedSet.newBuilder[K]
    for (t <- types) {
      builder ++= axisByType(t)
    }
    prev -- builder.result
  }

}