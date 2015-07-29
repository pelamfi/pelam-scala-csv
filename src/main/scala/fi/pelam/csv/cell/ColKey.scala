package fi.pelam.csv.cell

/**
 * Type for columns numbers used in parsed CSV data.
 *
 * This corresponds to Nth position separated by separator characters on each line in CSV text file.
 *
 * Most interesting functionality is the conversion of integer indices into alphabetical numbering
 * used in spreadsheet programs.
 *
 * @constructor Construct a column key object for given zero based column index integer.
 *
 * @param index zero based column number (column index).
 */
case class ColKey(index: Int) extends AxisKey[ColKey] {

  import ColKey._

  /**
   * Represents the column index as an one based integer as well as in alphabetical numbering used
   * in spreadsheet programs.
   */
  override def toString(): String = {
    s"Column ${toAlphabetical(index)} (${index})"
  }
}

object ColKey {

  /**
   * Converts integer into letters as is typically done for column numbers in spreadsheet programs.
   *
   * {{{ scala> ColKey.toAlphabetical(27)
   * res0: String = AB
   * }}}
   * @param index zero based column number
   */
  def toAlphabetical(index: Integer): String = {
    var i = index
    var result = ""

    val alphabetSize = 'Z' - 'A' + 1
    do {
      result = ('A' + i % alphabetSize).toChar + result
      // For most significant "digit" the radix is actually 27 because we can think empty string ""
      // as one of the digits.
      i = i / alphabetSize - 1
    } while (i >= 0)

    result
  }
}
