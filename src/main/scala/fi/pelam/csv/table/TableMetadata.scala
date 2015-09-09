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

import java.nio.charset.Charset

import fi.pelam.csv.CsvConstants

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
   * when reading CSV data or vice versa when writing CSV.
   */
  val charset: Charset = CsvConstants.defaultCharset

  /**
   * The separator for columns. Typically this is comma.
   */
  val separator: Char = CsvConstants.defaultSeparatorChar
}






