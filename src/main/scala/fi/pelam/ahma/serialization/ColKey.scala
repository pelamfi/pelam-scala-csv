package fi.pelam.ahma.serialization

case class ColKey(index: Int) extends Ordered[ColKey] {

  override def compare(that: ColKey): Int = this.index - that.index

  override def toString(): String = {
    var i = index
    var result = ""

    val alphabetSize = 'Z' - 'A' + 1
    do {
      result = ('A' + i % alphabetSize).toChar + result
      // For most significant "digit" the radix is actually 27 because we can think empty string ""
      // as one of the digits.
      i = i / alphabetSize - 1
    } while (i >= 0)
    s"Column $result (${index + 1})"
  }
}
