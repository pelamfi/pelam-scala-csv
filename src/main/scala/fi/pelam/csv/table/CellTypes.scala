package fi.pelam.csv.table

import java.util.Locale

import fi.pelam.csv.cell.{AxisKey, Cell, ColKey, RowKey}
import fi.pelam.csv.util.SortedBiMap

/**
 * This class is used internally by this CSV package to track mapping of rows and columns to their
 * user defined type objects. Also includes some other things that maybe should be elsewhere.
 *
 * @param rowTypes
 * @param colTypes
 * @param errors
 * @param locale is the cell type locale ie. the locale used in names in CSV data identifying types of each column and row
 * @tparam RT
 * @tparam CT
 */
// TODO: Refactor errors and locale somewhere else
case class CellTypes[RT, CT](
  rowTypes: SortedBiMap[RowKey, RT] = SortedBiMap[RowKey, RT](),
  colTypes: SortedBiMap[ColKey, CT] = SortedBiMap[ColKey, CT](),
  errors: Seq[TableReadingError] = IndexedSeq(),
  locale: Locale
  ) {

  import CellTypes._

  val rowCount = tableDimension(rowTypes.keys)

  val colCount = tableDimension(colTypes.keys)

  def getCellType(cell: Cell): Option[CellType[RT, CT]] = {
    for (rowType <- getRowType(cell);
         colType <- getColType(cell)) yield {
      CellType(rowType, colType)
    }
  }

  def getColType(cell: Cell): Option[CT] = {
    for (colType <- colTypes.get(cell.colKey)) yield colType
  }

  def getRowType(cell: Cell): Option[RT] = {
    for (rowType <- rowTypes.get(cell.rowKey)) yield rowType
  }

  def colsByType = colTypes.reverse

  def rowsByType = rowTypes.reverse

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

}

object CellTypes {
  def tableDimension(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))
}

