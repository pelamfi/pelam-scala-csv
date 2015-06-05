package fi.pelam.ahma.serialization

/**
 * Base type for RowKey and ColKey
 */
trait AxisKey[T <: AxisKey[_]] extends Ordered[T] {
  val index: Int

  override def compare(that: T): Int = this.index - that.index
}
