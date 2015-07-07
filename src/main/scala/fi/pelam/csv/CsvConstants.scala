package fi.pelam.csv

object CsvConstants {

  /**
   * Default separator in CSV is the comma as in "Comma Separated Values".
   *
   * However this canges depending on locale. Comma is not used in (some?)
   * locales where standard number formats use comma as the decimal separator.
   *
   * At least MS Excel with Finnish locale uses semicolon as separator.
   */
  val defaultSeparatorChar: Char = ','

  /**
   * Excel uses double quotes for cells containing the current separator character.
   * Two double quotes are used to encode a doulbe quote character in cell.
   */
  val quoteChar: Char = '"'

  /**
   * Double quote as string.
   */
  val quote: String = quoteChar.toString

  /**
   * Quote char escaped as two quotes.
   */
  val doubleQuote: String = quote + quote

}
