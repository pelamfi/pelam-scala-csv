package fi.pelam.ahma.serialization

sealed abstract class Cell {

  val cellKey: CellKey

  def rowKey = cellKey.row

  def colKey = cellKey.col

  def serializedString: String

}

case class SimpleCell(override val cellKey: CellKey,
  override val serializedString: String)
  extends Cell {

}
