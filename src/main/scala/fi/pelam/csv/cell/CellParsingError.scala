package fi.pelam.csv.cell

/**
 * - [[CellDeserializer]] subtypes produce these when they can't parse the cell content string.
 */
case class CellParsingError(msg: String) {

  override def toString() = s"Error parsing cell content: $msg"

}
