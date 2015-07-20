package fi.pelam.csv

/**
 * Type for row numbers used in parsed CSV data.
 *
 * This corresponds to zero based row number in CSV text file.
 *
 * @constructor Construct a row key object for given zero based row index integer.
 *
 * @param index zero based row number (row index).
 */
case class RowKey(index: Int) extends AxisKey[RowKey] {

  override def toString(): String = {
    s"Row ${index + 1}"
  }

}
