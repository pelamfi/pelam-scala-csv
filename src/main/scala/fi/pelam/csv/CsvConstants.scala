package fi.pelam.csv

import java.nio.charset.StandardCharsets

/**
 * This object contains some common CSV data related constants.
 */
object CsvConstants {

  /**
   * Default separator in CSV is the comma as in "Comma Separated Values".
   *
   * However this changes depending on locale. Comma is not used in (some?)
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

  /**
   * There is no really any de facto character set for CSV, but UTF-8 is
   * a good bet for anything.
   */
  val defaultCharset = StandardCharsets.UTF_8

}
