package fi.pelam.csv

case class RowKey(index: Int) extends AxisKey[RowKey] {

  override def toString(): String = {
    s"Row ${index + 1}"
  }

}
