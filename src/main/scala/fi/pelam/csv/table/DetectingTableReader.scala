/*
 * This file is part of pelam-scala-csv
 *
 * Copyright © Peter Lamberg 2015 (pelam-scala-csv@pelam.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.pelam.csv.table

import java.nio.charset.{StandardCharsets, Charset}
import fi.pelam.csv.CsvConstants
import java.io.ByteArrayInputStream
import java.util.Locale
import fi.pelam.csv.cell._

/**
 * This CSV format detection heuristic tries to read the input CSV
 * with varying parameters until a `Table` is produced with no errors or
 * all combinations are exhausted. In the latter case
 * the `Table` with least errors is returned.
 *
 * For simpler usage you can skip `initialMetadata` in the constructor
 * by using [[fi.pelam.csv.table.DetectingTableReader.apply]] defined in the companion object.
 *
 * {{{
 * ADD SAMPLE HERE FROM TEST SUITE
 * }}}
 *
 * @param initialMetadata base metadata instance. Copies with different format parameters will be created from this
 *                        using [[LocaleTableMetadata.withFormatParameters]].
 * @param tableReaderMaker user provided method that constructs a [[TableReader]] using
 *                         locales, separator and charset specified by [[LocaleTableMetadata]] parameter.
 * @param locales List of locales to try. Default is [[CsvConstants.commonLocales]].
 * @param charsets List of charsets to try. Default is [[CsvConstants.commonCharsets]].
 * @param separators List of separators to try. Default is [[CsvConstants.commonSeparators]].
 * @tparam RT The client specific row type.
 * @tparam CT The client specific column type.
 * @tparam M The type of the `metadata` parameter. Must be a sub type of [[TableMetadata]].
 *           This specifies the character set and separator to use when reading the CSV data from the input stream.
 */
// TODO: Scaladoc example, document parameters.
class DetectingTableReader[RT, CT, M <: LocaleTableMetadata[M]] (
  val initialMetadata: M,
  val tableReaderMaker: (M) => TableReader[RT, CT, M],
  val locales: Seq[Locale] = CsvConstants.commonLocales,
  val charsets: Seq[Charset] = CsvConstants.commonCharsets,
  val separators: Seq[Char] = CsvConstants.commonSeparators) {

  type ResultTable = Table[RT, CT, M]

  def read(): (ResultTable, TableReadingErrors) = {
    val readers = for (
      separator <- List(CsvConstants.defaultSeparatorChar, ';');
      charset <- List(StandardCharsets.UTF_8, StandardCharsets.ISO_8859_1);
      cellTypeLocale <- locales;
      dataLocale <- locales) yield {

        // http://stackoverflow.com/questions/8801818/polymorphic-updates-in-an-immutable-class-hierarchy
        // This is the cleanest way I can think of now...
        val metadata = initialMetadata.withFormatParameters(separator, charset, cellTypeLocale, dataLocale)

        tableReaderMaker(metadata)
      }

    val initialEvaluator = TableReaderEvaluator[RT, CT, M]()

    val finalEvaluator = readers.foldLeft(initialEvaluator)(_.evaluateReader(_))

    val resultOption = finalEvaluator.result

    resultOption.getOrElse(defaultRead())
  }

  /**
   * If there are no paramter combinations to evaluate, this is
   * used as the return value.
   */
  private def defaultRead() = {
    tableReaderMaker(initialMetadata).read()
  }

  /**
   * This method extends the usual read method with exception based error handling, which
   * may be useful in smaller applications that don't expect errors in input.
   *
   * A `RuntimeException` will be thrown when error is encountered.
   *
   */
  def readOrThrow(): ResultTable = {
    val (table, errors) = read()
    if (errors.noErrors) {
      table
    } else {
      sys.error(errors.toString)
    }
  }
}

object DetectingTableReader {

  /**
   * Custom constructor for using concrete class [[LocaleMetadata]]
   * instead of client defined [[LocaleTableMetadata]] subtype.
   *
   * @param tableReaderMaker user provided method that constructs a [[TableReader]] using
   *                         locales, separator and charset specified by [[LocaleTableMetadata]] parameter.
   * @param locales List of locales to try. Default is [[CsvConstants.commonLocales]].
   * @param charsets List of charsets to try. Default is [[CsvConstants.commonCharsets]].
   * @param separators List of separators to try. Default is [[CsvConstants.commonSeparators]].
   * @tparam RT The client specific row type.
   * @tparam CT The client specific column type.
   */
  def apply[RT, CT](
    tableReaderMaker: (LocaleMetadata) => TableReader[RT, CT, LocaleMetadata],
    locales: Seq[Locale] = CsvConstants.commonLocales,
    charsets: Seq[Charset] = CsvConstants.commonCharsets,
    separators: Seq[Char] = CsvConstants.commonSeparators) = {

    new DetectingTableReader(initialMetadata = LocaleMetadata(),
      tableReaderMaker = tableReaderMaker,
      locales = locales,
      charsets = charsets,
      separators = separators)
  }

}
