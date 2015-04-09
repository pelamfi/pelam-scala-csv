package fi.pelam.ahma.serialization

case class ColKey(index: Int) extends Ordered[ColKey] {

  override def compare(that: ColKey): Int = this.index - that.index
}
