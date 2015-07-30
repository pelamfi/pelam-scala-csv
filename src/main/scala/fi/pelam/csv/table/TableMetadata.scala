package fi.pelam.csv.table

import java.nio.charset.Charset
import java.util.Locale

import fi.pelam.csv.CsvConstants

// TODO: WIP, start using something like this internally or  remove this class
/**
 * Base class for metadata attached to [[Table]].
 *
 * Idea is that client code can extend this trait and piggyback whatever
 * extraneous data to [[Table]] instances.
 *
 * One example is the details of the CSV
 * format used. They are convenient to keep with the Table data in case user
 * needs to save a modified version of the original CSV file from which the data was read
 * from.
 *
 * Another use for this metadata mechanism is during the process of autodetecting
 * details of the CSV format by [[TableReader]].
 *
 * This trait has two variables that TableReader can use directly.
 *
 * For more complex format detection heuristics, this can be inherited
 * and extended with variables that a more custom detection algorithm then tries to detect.
 *
 */
trait TableMetadata {

  /**
   * The character set used to convert input bytes into a string
   * when reading CSV data.
   */
  val charset: Charset = CsvConstants.defaultCharset

  /**
   * The separator for columns. Typically this is comma.
   */
  val separator: Char = CsvConstants.defaultSeparatorChar

}

/**
 * Simplest implementation of [[TableMetadata]].
 */
case class SimpleTableMetadata(
  override val charset: Charset = CsvConstants.defaultCharset,
  override val separator: Char = CsvConstants.defaultSeparatorChar) extends TableMetadata

// TODO: Move to separate file
/**
 * @param dataLocale
 * @param cellTypeLocale
 */
case class LocaleTableMetadata(dataLocale: Locale,
  cellTypeLocale: Locale,
  override val charset: Charset = CsvConstants.defaultCharset,
  override val separator: Char = CsvConstants.defaultSeparatorChar) extends TableMetadata {

}
