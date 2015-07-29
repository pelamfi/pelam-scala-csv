package fi.pelam.csv

import java.nio.charset.Charset
import java.util.Locale

import fi.pelam.csv.cell._

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

object Table {

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
   * Main constructor for table. Typically this not used directly, but through [[TableReader]].
   *
   * @param charset Java charset used when the CSV data is serialized.
   * @param csvSeparator Separator character used when the CSV data is serialized.
   * @param dataLocale Locale used when encoding things like integers to the CSV file.
   * @param cellTypes Object that describes type of each column, row and cell using the client code defined objects.
   * @param cells The cells to be used in the table in any order.
   * @tparam RT Client specified object type used for typing rows in CSV data.
   * @tparam CT Client specified object type used for typing columns in CSV data.
   * @return constructed Table object
   */
  // TODO: Automatically fill in empty cells and never leave nulls in internal arrays
  def apply[RT, CT](charset: Charset,
    csvSeparator: Char,
    dataLocale: Locale,
    cellTypes: CellTypes[RT, CT],
    cells: TraversableOnce[Cell]): Table[RT, CT] = {

    val builtCells = buildCells(cells, cellTypes.rowCount, cellTypes.colCount)

    Table(charset, csvSeparator, dataLocale, cellTypes, builtCells)
  }

}

/**
 *
 * @param charset Java charset used when the CSV data is serialized.
 * @param csvSeparator Separator character used when the CSV data is serialized.
 * @param dataLocale Locale used when encoding things like integers to the CSV file.
 * @param cellTypes Object that describes type of each column, row and cell using the client code defined objects.
 * @param cells Fully populated 2D array of [[fi.pelam.csv.cell.Cell]] objects with matching dimensions to the ones specified in [[CellTypes]] instance.
 * @tparam RT Client specified object type used for typing rows in CSV data.
 * @tparam CT Client specified object type used for typing columns in CSV data.
 */
case class Table[RT, CT] private (charset: Charset,
  csvSeparator: Char,

  /**
   * Locale used in encoding eg. integer values into cells.
   *
   * This locale is mostly used via individual Cell types like [[fi.pelam.csv.cell.IntegerCell]]
   * which contain individual reference to the same Locale.
   */
  dataLocale: Locale,
  cellTypes: CellTypes[RT, CT],
  cells: IndexedSeq[IndexedSeq[Cell]]) {

  val rowCount: Int = cells.size

  val colCount: Int = cells.lift(0).map(_.size).getOrElse(0)

  for (row <- cells) {
    require(row.size == colCount, s"Same number of columns required for reach row. ${row.size} vs ${colCount}")
  }

  import Table._

  def rowTypes = cellTypes.rowTypes

  def colTypes = cellTypes.colTypes

  // Refactor to use ones in CellTypes

  def rowsByType: SortedMap[RT, IndexedSeq[RowKey]] = cellTypes.rowsByType

  def colsByType: SortedMap[CT, IndexedSeq[ColKey]] = cellTypes.colsByType

  def updatedCells(cells: TraversableOnce[Cell]): Table[RT, CT] = {
    var table = this
    for (cell <- cells) {
      table = table.updatedCell(cell)
    }
    table
  }

  def updatedCells(cells: Cell*): Table[RT, CT] = updatedCells(cells)

  def updatedCell(cell: Cell): Table[RT, CT] = {
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
  def getSingleColByType(colType: CT) = cellTypes.getSingleColByType(colType)

  /**
   * Throws if the number of rows with given type is not 1
   */
  def getSingleRowByType(rowType: RT) = cellTypes.getSingleRowByType(rowType)

  /**
   * Get cells from single column of colType for each row of rowType.
   *
   * Throws if there are multiple columns with CT
   */
  def getSingleCol(colType: CT, rowType: RT): IndexedSeq[Cell] = {
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
