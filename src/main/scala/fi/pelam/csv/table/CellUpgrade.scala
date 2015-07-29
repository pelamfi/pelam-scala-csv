package fi.pelam.csv.table

import fi.pelam.csv.cell.{Cell, CellKey}

/**
 * A base class for client defined factories which are invoked based on [[CellType CellTypes]] of each cell.
 * The invocation of instances of subclasses of this class are handled by the [[TableReader]].
 *
 * The factory can then replace the cell with another instance. Intended use of this class is to upgrade
 * the types of cells from [[fi.pelam.csv.cell.StringCell StringCell]]s to more specialized subclasses of
 * [[fi.pelam.csv.cell.Cell]] like the [[fi.pelam.csv.cell.IntegerCell IntegerCell]].
 *
 * Another function that can be performed at the same time is validating that the contents of the cells against
 * client specified requirements.
 */
abstract class CellUpgrade {
  def fromString(cellKey: CellKey, locale: java.util.Locale, input: String): Either[TableReadingError, Cell]
}
