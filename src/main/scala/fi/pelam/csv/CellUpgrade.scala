package fi.pelam.csv

import fi.pelam.csv.cell.{StringCell, Cell, CellKey, IntegerCell}

/**
 * A base class for client defined factories which are invoked based on [[CellType]] of each cell.
 * The invocation of instances of subclasses of this class are handled by [[TableReader]].
 *
 * The factory can then replace the cell with another instance. Intended use of this class is to upgrade
 * the types of cells from [[fi.pelam.csv.cell.StringCell]] to more specialized subclasses of
 * [[fi.pelam.csv.cell.Cell]] like [[fi.pelam.csv.cell.IntegerCell]].
 *
 * Another function that can be performed at the same time is validating that the contents of the cells against
 * client specified requirements.
 */
abstract class CellUpgrade {
  def fromString(cellKey: CellKey, locale: java.util.Locale, input: String): Either[TableReadingError, Cell]
}
