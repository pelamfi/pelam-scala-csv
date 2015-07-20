package fi.pelam.csv

/**
 * Base class for cell objects in [[Table]].
 *
 * [[CsvReader]] outputs instances of Cell subclass [[StringCell]].
 */
abstract class Cell {

  val cellKey: CellKey

  def rowKey = cellKey.rowKey

  def colKey = cellKey.colKey

  def serializedString: String

}
