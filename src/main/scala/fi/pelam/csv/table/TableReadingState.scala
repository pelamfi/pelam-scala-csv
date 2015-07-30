package fi.pelam.csv.table

import fi.pelam.csv.cell._
import fi.pelam.csv.util.SortedBiMap

case class TableReadingState[RT, CT](cells: IndexedSeq[Cell] = IndexedSeq(),
  rowTypes: TableReader.RowTypes[RT] = SortedBiMap[RowKey, RT](),
  colTypes: TableReader.ColTypes[CT] = SortedBiMap[ColKey, CT](),
  errors: TableReadingErrors = TableReadingErrors()) {

  def isSuccess: Boolean = errors.noErrors

  def defineRowType(row: RowKey, rowType: RT): TableReadingState[RT, CT] = copy(rowTypes = rowTypes.updated(row, rowType))

  def defineColType(col: ColKey, colType: CT): TableReadingState[RT, CT] = copy(colTypes = colTypes.updated(col, colType))

  def addErrors(moreErrors: Iterator[TableReadingError]): TableReadingState[RT, CT] = copy(errors = errors.add(moreErrors))

  def addError(error: TableReadingError): TableReadingState[RT, CT] = copy(errors = errors.add(error))
}
