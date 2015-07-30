package fi.pelam.csv.cell

/**
 * Base type for [[fi.pelam.csv.cell.RowKey RowKey]] and [[fi.pelam.csv.cell.ColKey ColKey]].
 * Provides index and ordering.
 *
 * Ordering is simply the natural order of the index values.
 */
trait AxisKey[T <: AxisKey[_]] extends Ordered[T] {
  val index: Int

  override def compare(that: T): Int = this.index - that.index
}
