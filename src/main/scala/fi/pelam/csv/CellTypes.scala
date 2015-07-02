package fi.pelam.csv

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import scala.collection.SortedMap

case class CellTypes[RT, CT](
  rowTypes: SortedMap[RowKey, RT] = SortedMap[RowKey, RT](),
  colTypes: SortedMap[ColKey, CT] = SortedMap[ColKey, CT](),
  errors: Seq[TableReadingError] = IndexedSeq(),
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

}
