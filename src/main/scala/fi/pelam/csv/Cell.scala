package fi.pelam.csv

abstract class Cell {

  val cellKey: CellKey

  def rowKey = cellKey.rowKey

  def colKey = cellKey.colKey

  def serializedString: String

}
