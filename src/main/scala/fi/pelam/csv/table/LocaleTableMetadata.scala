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
import java.util.Locale

// TODO: Scaladoc
trait LocaleTableMetadata[T <: LocaleTableMetadata[T]] extends TableMetadata {
  /**
   * Locale used in cell and column type names stored in CSV cells.
   */
  val cellTypeLocale: Locale = Locale.ROOT

  /**
   * Locale used in encoding data in CSV cells. This accounts
   * for example for different thousand separators etc.
   * used in diffent locales by spreadsheet progrms.
   */
  val dataLocale: Locale = Locale.ROOT

  /**
   * This is a polymorphic way of accessing the concrete case class copy.
   */
  def withFormatParameters(separator: Char, charset: Charset, cellTypeLocale: Locale, dataLocale: Locale): T
}
