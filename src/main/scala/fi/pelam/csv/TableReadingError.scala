package fi.pelam.csv

case class TableReadingError(msg: String, cell: Option[Cell] = None) {

  override def toString() = {
    if (cell.isDefined) {
      msg + " " + cell
    } else {
      msg
    }
  }

  def addedDetails(specifiedCell: Cell, msgAppend: Any = ""): TableReadingError = {
    copy(cell = Some(cell.getOrElse(specifiedCell)), msg = msg + msgAppend.toString)
  }

}
