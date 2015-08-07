package fi.pelam.csv.cell

/**
 * [[CellParser]] subtypes produce these errors when they can't parse the cell content string.
 */
case class CellParsingError(msg: String) {

  override def toString() = s"Error parsing cell content: $msg"

}
