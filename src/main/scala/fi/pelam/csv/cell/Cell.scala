package fi.pelam.csv.cell

/**
 * Base class for cells used in this CSV library.
 *
 * The two main APIs of this library
 * [[fi.pelam.csv.table.TableReader the table oriented]] and the [[fi.pelam.csv.stream.CsvReader stream oriented]]
 * use subclasses of this class to represent the data.
 *
 * One important use for this class is in [[fi.pelam.csv.stream.CsvReader CsvReader]].
 * CsvReader outputs instances of [[fi.pelam.csv.cell.StringCell StringCell]] which is
 * a subclass of this class.

 * @see See the documentation of cellKey member below for design rationale
 *      on having coordinates in each cell.
 */
abstract class Cell {

  /**
   * Each cell directly contains information about its coordinates in the CSV data.
   *
   * The rationale is that this makes processing streams of cells simpler and allows
   * for detecting many types of errors. Also when problems are detected the coordinates
   * of the problematic cell can easily be included in the error messages.
   *
   * It is true that in most cases the coordinates of the cell would be known from surrounding
   * data structures. However if we relied solely on that, there would not be an easy uniform
   * way for code operating on cells to know coordinates of each cell.
   *
   * Another downside is that cell instances can't be reused in different places in data
   * in style of the flyweight pattern.
   */
  val cellKey: CellKey

  /**
   * Shortcut to [[CellKey.rowKey]]
   */
  def rowKey = cellKey.rowKey

  /**
   * Shortcut to [[CellKey.colKey]]
   */
  def colKey = cellKey.colKey

  /**
   * The data as it would be represented in CSV file on
   * disk sans quoting.
   *
   * Subclasses of this Cell type should provide more meaningful ways
   * of accessing data.
   *
   * @see [[IntegerCell IntegerCell]]
   */
  def serializedString: String

}
