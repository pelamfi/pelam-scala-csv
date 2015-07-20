package fi.pelam.csv

/**
 * Coordinates of a [[Cell]] in a [[Table]].
 *
 * Basically a composition of [[RowKey]] and [[ColKey]] or alternatively
 * two zero based integers [[rowIndex]] and [[colIndex]].
 *
 * [[Cell]] has a CellKey as a member.
 *
 * == Cell coordinates ==
 *
 * Columns and rows are thought to start from top left corner.
 *
 * Typically in spreadsheet programs columns are numbered with an alphabetical
 * scheme and rows number is a one based integer.
 *
 * == Ordering ==
 *
 * CellKeys are ordered first in increasing row order and then in increasing
 * column order.
 *
 * The object representations provide type safety and spread sheet style
 * column and row numbering.
 */
case class CellKey(rowIndex: Int, colIndex: Int) extends Ordered[CellKey] {

  /**
   * Method for advancing one row.
   */
  def nextRow: CellKey = CellKey(rowIndex + 1, 0)

  /**
   * Method for advancing one column.
   */
  def nextCol: CellKey = CellKey(rowIndex, colIndex + 1)

  /**
   * Extracts the row key component.
   */
  val rowKey = RowKey(rowIndex)

  /**
   * Extracts the column key component.
   */
  val colKey = ColKey(colIndex)

  // TODO: Remove superficial method?
  def indices = (rowIndex, colIndex)

  /**
   * Example: {{{
   *   scala> CellKey(26, 26)
   *   cellKey: fi.pelam.csv.CellKey = Row 26, Column AA (26)
   * }}}
   */
  override def toString(): String = s"$rowKey, $colKey"

  /**
   * See section on ordering at class top level documentation.
   */
  override def compare(that: CellKey): Int = {
    // http://stackoverflow.com/a/19348339/1148030
    import scala.math.Ordered.orderingToOrdered
    (this.rowIndex, this.colIndex) compare(that.rowIndex, that.colIndex)
  }
}

object CellKey {
  def apply(rowKey: RowKey, colIndex: Int): CellKey = CellKey(rowKey.index, colIndex)

  def apply(rowIndex: Int, colKey: ColKey): CellKey = CellKey(rowIndex, colKey.index)

  def apply(rowKey: RowKey, colKey: ColKey): CellKey = CellKey(rowKey.index, colKey.index)
}
