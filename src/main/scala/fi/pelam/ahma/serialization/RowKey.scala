package fi.pelam.ahma.serialization

case class RowKey(index: Int) extends Ordered[RowKey] {

  override def compare(that: RowKey): Int = this.index - that.index

}
