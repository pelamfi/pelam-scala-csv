package fi.pelam.csv

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.collection.SortedMap

case class CellTypes[RT, CT](
  rowTypes: BiMap[RowKey, RT] = BiMap[RowKey, RT](SortedMap[RowKey, RT]()),
  colTypes: BiMap[ColKey, CT] = BiMap[ColKey, CT](SortedMap[ColKey, CT]()),
  errors: Seq[TableReadingError] = IndexedSeq(),
  /**
   * Locale used in names in CSV data identifying types of each column and row
   */
  cellTypesLocale: Locale
  ) {

  def getCellType(cell: Cell): Option[CellType[RT, CT]] = {
    for(rowType <- getRowType(cell);
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

