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

package fi.pelam.csv

import java.nio.charset.{Charset, StandardCharsets}
import java.util.Locale

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
   * a good place to start with for anything.
   */
  val defaultCharset = StandardCharsets.UTF_8

  /**
   * Constant for `commonLocales`.
   */
  val localeFi: Locale = Locale.forLanguageTag("FI")

  /**
   * Pretty much ad hoc list of locales that could be common. If you
   * have a better idea what should be here, let me know!
   *
   * Idea is that this contains a small set of guesses that together
   * would cover a large fraction of real world cases. Full coverage
   * is not feasible so only locales with presumed relatively large
   * user base should be included.
   *
   * This is list is the default set of attempted locales in
   * [[fi.pelam.csv.table.DetectingTableReader]].
   *
   * If this library is used in some rare context, then the defaults
   * provided in this class will most likely have to be overridden
   * appropriately by client code.
   */
  val commonLocales: IndexedSeq[Locale] = IndexedSeq(
    Locale.ROOT,
    Locale.FRENCH,
    localeFi)

  /**
   * Somewhat ad hoc list of charsets that could be common. If you
   * have a better idea what should be here, let me know!
   * Cyrillic? Asian?
   *
   * Idea is that this contains a small set of guesses that together
   * would cover a large fraction of real world cases. Full coverage
   * is not feasible so only charsets with presumed relatively large
   * user base should be included.
   *
   * This is list is the default set of attempted locales in
   * [[fi.pelam.csv.table.DetectingTableReader]].
   *
   * If this library is used in some rare context, then the defaults
   * provided in this class will most likely have to be overridden
   * appropriately by client code.
   */
  val commonCharsets: IndexedSeq[Charset] = IndexedSeq(
    // UTF-8
    defaultCharset,
    // Typical modern windows charset.
    StandardCharsets.UTF_16LE,
    // The grand old one.
    StandardCharsets.US_ASCII,
    // Typical European charset, also covers pure ascii.
    StandardCharsets.ISO_8859_1)

  /**
   * Somewhat ad hoc list of charsets that could be common. If you
   * have a better idea what should be here, let me know!
   * Cyrillic? Asian?
   *
   * Idea is that this contains a small set of guesses that together
   * would cover a large fraction of real world cases. Full coverage
   * is not feasible so only separator with presumed relatively
   * large user base should be included.
   *
   * This is list is the default set of attempted locales in
   * [[fi.pelam.csv.table.DetectingTableReader]].
   *
   * If this library is used in some rare context, then the defaults
   * provided in this class will most likely have to be overridden
   * appropriately by client code.
   */
  val commonSeparators: IndexedSeq[Char] = IndexedSeq(
    // Comma
    defaultSeparatorChar,
    ';',
    '\t')


}
