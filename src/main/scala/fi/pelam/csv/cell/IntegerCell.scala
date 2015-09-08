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

import java.text.{NumberFormat, ParseException, ParsePosition}
import java.util.Locale

import fi.pelam.csv.util.FormatterUtil
import fi.pelam.csv.util.FormatterUtil.Formatter

// @formatter:off IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
/**
 * Basically this class is a sample implementation of a more specialised subtype of
 * [[fi.pelam.csv.cell.Cell]].
 *
 * It is expected that any nontrivial client will want to specify its own subtypes
 * of [[fi.pelam.csv.cell.Cell]].
 *
 * This class is quite simple, but the companion object is more
 * interesting as it implements the [[CellParser]] trait and acts as a factory
 * which produces IntegerCell instances (or errors if parsing fails) from String data.
 *
 * @param cellKey the location of this cell in a CSV table.
 * @param formatter A function used to convert the integer held by this cell into a `String`
 *                  to be stored in CSV text data.
 * @param value is the integer stored in CSV.
 */
// @formatter:on IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
case class IntegerCell(override val cellKey: CellKey,
  override val value: Int)
  (implicit override val formatter: Formatter[Int] = IntegerCell.defaultFormatter)
  extends Cell with NumberCell[Int] {

}

/**
 * The IntegerCell class it self is quite simple, but this companion object is more
 * interesting as it implements the [[CellParser]] trait and acts as a factory
 * which produces IntegerCell instances (or errors if parsing fails) from String data.
 *
 * This companion object can be used used to upgrade cells in TableReader in an easy way
 * by using it in a map passed to [[fi.pelam.csv.table.TableReaderConfig.makeCellUpgrader]].
 * to specify which cells should be interpreted as containing integers.
 */
object IntegerCell {
  import fi.pelam.csv.util.FormatterUtil._

  def defaultFormatter = toSynchronizedFormatter[Int](NumberFormat.getInstance(Locale.ROOT))

  val defaultParser = parserForLocale(Locale.ROOT)

  def parserForLocale(locale: Locale): CellParser = {
    parserForNumberFormat(NumberFormat.getInstance(locale))
    /*
    result match {
      case Left(e: CellParsingError) => Left(e.withExtraMessage(s"Used locale $locale"))
      case _ => _
    }*/
  }

  def parserForNumberFormat(numberFormat: NumberFormat): CellParser = {
    new CellParser {

      val parser = toSynchronizedParser(numberFormat)

      val formatter = toSynchronizedFormatter[Int](numberFormat)

      override def parse(cellKey: CellKey, input: String) = {

        val trimmedInput = input.trim()

        val result = parser(trimmedInput)

        result match {
          case Some(number) => {
            val intValue = number.intValue()

            if (intValue == number) {
              Right(IntegerCell(cellKey, intValue)(formatter))
            } else {
              Left(CellParsingError(s"Expected integer, but value '$input' is decimal"))
            }
          }
          case None => Left(CellParsingError(s"Expected integer, but input '$input' " +
            s"could not be fully parsed."))
        }
      }
    }
}
}


