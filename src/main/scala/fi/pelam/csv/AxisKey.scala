package fi.pelam.csv

/**
 * Base type for [[RowKey]] and [[ColKey]]. Provides index and ordering.
 *
 * Ordering is simply the natural order of the index values.
 */
trait AxisKey[T <: AxisKey[_]] extends Ordered[T] {
  val index: Int

  override def compare(that: T): Int = this.index - that.index
}
