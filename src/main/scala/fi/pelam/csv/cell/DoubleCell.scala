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
import fi.pelam.csv.util.FormatterUtil._

/**
 * See documentation on [[IntegerCell]]. This is basically same, but with `Double` instead of Int.
 */
case class DoubleCell(override val cellKey: CellKey,
  override val value: Double)
  (implicit val formatter: DoubleCell.Formatter = DoubleCell.defaultFormatter)
  extends Cell {

  def serializedString: String = {
    formatter(value)
  }
}

/**
 * See documentation on [[IntegerCell]]. This is basically same, but with `Double` instead of Int.
 */
object DoubleCell {

  type Formatter = FormatterUtil.Formatter[Double]

  def defaultFormatter = toSynchronizedFormatter[Double](NumberFormat.getInstance(Locale.ROOT))

  def parserForLocale(locale: Locale): Cell.Parser = {
    val numberFormat = NumberFormat.getInstance(locale)
    val parser = parserForNumberFormat(numberFormat)
    CellParserUtil.addLocaleToError(parser, locale)
  }

  def parserForNumberFormat(numberFormat: NumberFormat): Cell.Parser = {

    val parser = toSynchronizedParser(numberFormat)

    val formatter = toSynchronizedFormatter[Double](numberFormat)

    { (cellKey: CellKey, input: String) =>
      val trimmedInput = input.trim

      parser(trimmedInput) match {
        case Some(number) => {
          Right(DoubleCell(cellKey, number.doubleValue())(formatter))
        }
        case None => Left(CellParsingError(s"Expected decimal number (double precision), " +
          s"but input '$input' could not be fully parsed.."))
      }
    }
  }
}

