package fi.pelam.csv

import java.nio.charset.Charset
import java.util.Locale

import scala.collection.SortedMap
import scala.collection.immutable.TreeMap

object Table {
  val rowTypeCol = ColKey(0)


  // http://stackoverflow.com/a/24222250/1148030
  // TODO: refactor reverseMap
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

  // TODO: Move to CellTypes
  def tableSize(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  def apply[RT, CT](charset: Charset,
    csvSeparator: Char,
    dataLocale: Locale,
    cellTypes: CellTypes[RT, CT],
    cells: TraversableOnce[Cell]): Table[RT, CT] = {

    val builtCells = buildCells(cells, tableSize(cellTypes.rowTypes.keys), tableSize(cellTypes.colTypes.keys))

    Table(charset, csvSeparator, dataLocale, cellTypes, builtCells)
  }

}

case class Table[RT, CT](charset: Charset,
  csvSeparator: Char,

  /**
   * Locale used in encoding eg. integer values into cells.
   *
   * This locale is mostly used via individual Cell types like [[IntegerCell]]
   * which contain individual reference to the same Locale.
   */
  dataLocale: Locale,
  cellTypes: CellTypes[RT, CT],
  cells: IndexedSeq[IndexedSeq[Cell]]) {

  import Table._

  def rowTypes = cellTypes.rowTypes

  def colTypes = cellTypes.colTypes

  // Refactor to use ones in CellTypes

  def rowsByType: Map[RT, IndexedSeq[RowKey]] = cellTypes.rowsByType

  def colsByType: Map[CT, IndexedSeq[ColKey]] = cellTypes.colsByType

  // TODO: Move to CellTypes
  val rowCount: Int = rowTypes.keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

  // TODO: Move to CellTypes
  val colCount: Int = colTypes.keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))

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

  // TODO: Use ones in CellTypes
  /**
   * Throws if the number of columns with given type is not 1
   */
  def getSingleColByType(colType: CT): ColKey = {
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
  def getSingleRowByType(rowType: RT): RowKey = {
    val rows = rowsByType(rowType)

    if (rows.size == 0) {

      sys.error(s"Expected 1 row of type $rowType but no rows of that type found.")

    } else if (rows.size > 1) {

      sys.error(s"Expected 1 row of type $rowType but more than 1 found. " +
        rows.tail.foldLeft(rows.head.toString)(_ + ", " + _))

    } else {
      rows(0)
    }
  }

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
