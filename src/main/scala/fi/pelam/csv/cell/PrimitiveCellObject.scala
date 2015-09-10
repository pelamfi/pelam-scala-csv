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

package fi.pelam.csv.cell

import java.text.NumberFormat
import java.util.Locale

import fi.pelam.csv.util.FormatterUtil

trait PrimitiveCellObject[T <: AnyVal] {
  import fi.pelam.csv.util.FormatterUtil._

  final type NumberFormatter = FormatterUtil.Formatter[T]
  
  final type Parser = Cell.Parser

  final val defaultFormatter = toSynchronizedFormatter[T](NumberFormat.getInstance(Locale.ROOT))

  final val defaultParser = parserForLocale(Locale.ROOT)

  val primitiveDescription: String

  def numberToCell(cellKey: CellKey, input: String, number: Number, formatter: NumberFormatter): Cell.ParserResult

  final def parserForNumberFormat(numberFormat: NumberFormat): Cell.Parser = {
    val parser = toSynchronizedParser(numberFormat)
    val formatter = toSynchronizedFormatter[T](numberFormat)

    { (cellKey: CellKey, input: String) =>
      parser(input.trim) match {
        case Some(number) => numberToCell(cellKey, input, number, formatter)
        case None => Left(CellParsingError(s"Expected $primitiveDescription, but input '$input' " +
          s"could not be fully parsed."))
      }
    }
  }

  final def parserForLocale(locale: Locale): Cell.Parser = {
    val numberFormat = NumberFormat.getInstance(locale)
    val parser = parserForNumberFormat(numberFormat)
    CellParserUtil.addLocaleToError(parser, locale)
  }
}
