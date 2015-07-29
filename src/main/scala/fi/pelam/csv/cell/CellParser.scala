package fi.pelam.csv.cell

/**
 * A base class for client defined [[fi.pelam.csv.cell.Cell Cell]] parsers
 * which construct cells from strings.
 *
 * == Use of this class in [[fi.pelam.csv.table.TableReader TableReader]] ==
 *
 * These factories are used in [[fi.pelam.csv.table.TableReader TableReader]].
 * The factories are invoked based on [[fi.pelam.csv.table.CellType CellTypes]] of each cell.
 *
 * The factory can then replace the cell with an instance of another subclass of [[fi.pelam.csv.cell.Cell Cell]].
 * The purpose of the mechanism in [[fi.pelam.csv.table.TableReader TableReader]] is to upgrade the
 * cells from simple [[fi.pelam.csv.cell.StringCell StringCell]]s to more specialized subclasses of
 * [[fi.pelam.csv.cell.Cell Cell]] like the [[fi.pelam.csv.cell.IntegerCell IntegerCell]].
 *
 * Another function that can be performed at the same time is validating that the contents of the cells against
 * client specified requirements.
 */
abstract class CellParser {
  def parse(cellKey: CellKey, locale: java.util.Locale, input: String): Either[CellParsingError, Cell]
}
