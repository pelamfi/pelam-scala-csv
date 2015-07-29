package fi.pelam.csv.table

import fi.pelam.csv.cell.{CellParsingError, CellParser, Cell}

/**
 * - Various phases in [[TableReader]] produce these when building a Table object from input fails.
 * - [[fi.pelam.csv.cell.CellParsingError CellParsingErrors]] errors are converted to these errors in [[TableReader]].
 */
case class TableReadingError(msg: String, cell: Option[Cell] = None) {

  override def toString() = {
    if (cell.isDefined) {
      msg + " " + cell.get
    } else {
      msg
    }
  }

  def addedDetails(specifiedCell: Cell, msgAppend: String = ""): TableReadingError = {
    copy(cell = Some(cell.getOrElse(specifiedCell)), msg = msg + msgAppend)
  }

}

object TableReadingError {

  def apply(innerError: CellParsingError, cell: Cell, cellType: CellType[_, _]): TableReadingError = {
    TableReadingError(s"$innerError $cellType", Some(cell))
  }

}
