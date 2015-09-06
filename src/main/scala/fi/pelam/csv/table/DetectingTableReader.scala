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

import java.nio.charset.{Charset, StandardCharsets}
import java.util.Locale

import fi.pelam.csv.CsvConstants

/**
 * This CSV format detection heuristic tries to read the input CSV
 * with varying parameters until a `Table` is produced with no errors or
 * all combinations are exhausted. In the latter case
 * the `Table` with least errors is returned.
 *
 * For simpler usage you can skip `initialMetadata` in the constructor
 * by using [[fi.pelam.csv.table.DetectingTableReader.apply the apply method]]
 * defined in the companion object.
 *
 * The interface of this class is similar to the one in [[TableReader]], but this
 * class creates multiple `TableReader` instances under the hood.
 *
 * == Example on detection of CSV format ==
 *
 * This example detects and parses a weird CSV format in which the separator
 * is the one used at least in the finnish locale, but numeric data is
 * formatted in english style. The column types are defined
 * on the first row and the row type is defined by the first column.
 *
 * {{{
 *   import fi.pelam.csv.table._
 *   import fi.pelam.csv.cell._
 *   import TableReaderConfig._
 *
 *   val validColTypes = Set("header", "name", "number")
 *
 *   val reader = DetectingTableReader[String, String](
 *
 *     tableReaderMaker = { (metadata) => new TableReader(
 *       openStream = "header;name;number\n" +
 *         "data;foo;1,234.0\n" +
 *         "data;bar;1,234,567.89",
 *
 *       tableMetadata = metadata,
 *
 *       rowTyper = makeRowTyper({
 *         case (CellKey(_, 0), rowType) => rowType
 *       }),
 *
 *       // Column type is specified by the first row.
 *       // Type names are checked and error is generated for unknown
 *       // column types by errorOnUndefinedCol.
 *       // This strictness is what enables the correct detection of CSV format.
 *       colTyper = errorOnUndefinedCol(makeColTyper({
 *         case (CellKey(0, _), colType) if validColTypes.contains(colType) => colType
 *       })),
 *
 *       cellUpgrader = makeCellUpgrader({
 *         case CellType("data", "number") => DoubleCell
 *       },
 *       metadata.dataLocale
 *       ))
 *     }
 *   )
 *
 *   val table = reader.readOrThrow()
 *
 *   // Get values from cells in column with type "name" on rows with type "data."
 *   table.getSingleCol("data", "name").map(_.value).toList
 *   // Will give List("foo","bar")
 *
 *   // Get values from cells in column with type "number" on rows with type "data."
 *   table.getSingleCol("number", "data").map(_.value).toList)
 *   // Will give List(1234, 1234567.89)
 * }}}
 *
 * @param initialMetadata base metadata instance. Copies with different format parameters will be created from this
 *                        using [[LocaleTableMetadata.withFormatParameters]]. Idea is that you client
 *                        have custom metadata subclass and use this parameter to set initial values for
 *                        custom fields in the metadata.
 *
 * @param tableReaderMaker user provided method that constructs a [[TableReader]] using
 *                         locales, separator and charset specified by [[LocaleTableMetadata]] parameter.
 *
 * @param locales List of locales to try for `cellTypeLocale` and `dataLocale`.
 *                The default is [[CsvConstants.commonLocales]].
 *
 * @param charsets List of charsets to try. Default is [[CsvConstants.commonCharsets]].
 *
 * @param separators List of separators to try. Default is [[CsvConstants.commonSeparators]].
 *
 * @tparam RT The client specific row type.
 *
 * @tparam CT The client specific column type.
 *
 * @tparam M The type of the `metadata` parameter. Must be a sub type of [[LocaleTableMetadata]].
 *           This is used to manage the character set, separator, `cellTypeLocale` and `dataLocale`
 *           combinations when attempting to read the CSV data from the input stream.
 */
class DetectingTableReader[RT, CT, M <: LocaleTableMetadata[M]] (
  val initialMetadata: M,
  val tableReaderMaker: (M) => TableReader[RT, CT, M],
  val locales: Seq[Locale] = CsvConstants.commonLocales,
  val charsets: Seq[Charset] = CsvConstants.commonCharsets,
  val separators: Seq[Char] = CsvConstants.commonSeparators) {

  type ResultTable = Table[RT, CT, M]

  /**
   * The main method in this class. Can be called several times.
   * The input stream is may be opened and closed many times
   * per each call.
   *
   * If there are no errors [[TableReadingErrors.noErrors]] is `true`.
   *
   * @return a pair with a [[.TableReader]] and [[TableReadingErrors]].
   */
  def read(): (ResultTable, TableReadingErrors) = {

    // TODO: Is there a functional way to combine TableReaderEvaluator and the generation of CSV format combinations.
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
   * This method extends the basic `read` method with exception based error handling,
   * which may be useful in smaller applications that don't expect or handle
   * errors in input.
   *
   * A `RuntimeException` will be thrown when error is encountered.
   */
  def readOrThrow(): ResultTable = {
    val (table, errors) = read()
    if (errors.noErrors) {
      table
    } else {
      sys.error(errors.toString)
    }
  }

  // If there are no parameter combinations to evaluate, this is used as the return value.
  private def defaultRead() = {
    tableReaderMaker(initialMetadata).read()
  }

}

object DetectingTableReader {

  /**
   * Custom constructor for using concrete class [[LocaleMetadata]]
   * instead of client defined [[LocaleTableMetadata]] subtype.
   *
   * @param tableReaderMaker user provided method that constructs a [[TableReader]] using
   *                         locales, separator and charset specified by [[LocaleTableMetadata]] parameter.
   *
   * @param locales List of locales to try. Default is [[CsvConstants.commonLocales]].
   *
   * @param charsets List of charsets to try. Default is [[CsvConstants.commonCharsets]].
   *
   * @param separators List of separators to try. Default is [[CsvConstants.commonSeparators]].
   *
   * @tparam RT The client specific row type.
   *
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
