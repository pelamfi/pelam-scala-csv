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
   * There is no really any de facto character for CSV, but UTF-8 is
   * a good place to start with for anything.
   */
  val defaultCharset = StandardCharsets.UTF_8

}
