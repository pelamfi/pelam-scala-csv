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

import fi.pelam.csv.CsvConstants

/**
 *
 *
 * @param dataLocale
 * @param cellTypeLocale
 */
// TODO: Scaladoc
case class LocaleMetadata(override val dataLocale: Locale = Locale.ROOT,
  override val cellTypeLocale: Locale = Locale.ROOT,
  override val charset: Charset = CsvConstants.defaultCharset,
  override val separator: Char = CsvConstants.defaultSeparatorChar) extends LocaleTableMetadata[LocaleMetadata] {

  /**
   * This is a polymorphic way of accessing the concrete case class copy.
   */
  override def withFormatParameters(separator: Char,
    charset: Charset,
    cellTypeLocale: Locale,
    dataLocale: Locale): LocaleMetadata = {

    copy(separator = separator, charset = charset, cellTypeLocale = cellTypeLocale, dataLocale = dataLocale)
  }
}
