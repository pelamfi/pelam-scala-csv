package fi.pelam.csv

/**
 * A base class for client defined factories which are invoked based on [[CellType]] of each cell.
 * The invocation of instances of subclasses of this class are handled by [[TableReader]].
 *
 * The factory can then replace the cell with another instance. Intended use of this class is to upgrade
 * the types of cells from [[StringCell]] to more specialized subclasses of [[Cell]] like [[IntegerCell]].
 *
 * Another function that can be performed at the same time is validating that the contents of the cells against
 * client specified requirements.
 */
abstract class CellUpgrade {
  def fromString(cellKey: CellKey, locale: java.util.Locale, input: String): Either[TableReadingError, Cell]
}
