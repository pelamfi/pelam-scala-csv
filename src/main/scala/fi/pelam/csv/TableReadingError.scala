package fi.pelam.csv

import fi.pelam.csv.cell.Cell

/**
 * - [[CellUpgrade]] subtypes produce these when they can't upgrade the cell.
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
