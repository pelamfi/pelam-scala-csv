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

/**
 * Basically this class is a sample implementation of a more specialised subtype of
 * [[fi.pelam.csv.cell.Cell]].
 *
 * It is expected that any nontrivial client will want to specify its own subtypes
 * of [[fi.pelam.csv.cell.Cell]].
 *
 * This class is quite simple, but the companion object is more
 * interesting as it implements the [[CellParser]] trait and acts as a factory
 * which produces DoubleCell instances (or errors if parsing fails) from String data.
 *
 * @param cellKey the location of this cell in a CSV table.
 * @param formatter A function used to convert the integer held by this cell into a `String`
 *                  to be stored in CSV text data.
 * @param value is the integer stored in CSV.
 */
// TODO: Fix DRY wrt IntegerCell
case class DoubleCell(override val cellKey: CellKey,
  override val value: Double)
  (implicit val formatter: DoubleCell.Formatter = DoubleCell.defaultFormatter)
  extends Cell {

  def serializedString: String = {
    formatter(value)
  }
}

/**
 * The DoubleCell class it self is quite simple, but this companion object is more
 * interesting as it implements the [[CellParser]] trait and acts as a factory
 * which produces DoubleCell instances (or errors if parsing fails) from String data.
 *
 * This companion object can be used used to upgrade cells in TableReader in an easy way
 * by using it in a map passed to [[fi.pelam.csv.table.TableReader.defineCellUpgrader]].
 * to specify which cells should be interpreted as containing integers.
 */
object DoubleCell extends CellParser {

  type Formatter = (Double) => String

  def defaultFormatter = toSynchronizedFormatter(NumberFormat.getInstance(Locale.ROOT))

  /**
   * This function is a workaround for the Java feature of `NumberFormat`
   * not being thread safe. http://stackoverflow.com/a/1285353/1148030
   *
   * This transformation returns a thread safe Formatter based on the
   * `NumberFormat` passed in as parameter.
   */
  def toSynchronizedFormatter(numberFormat: NumberFormat): Formatter = {

    // Clone to ensure instance is only used in this scope
    val clonedFormatter = numberFormat.clone().asInstanceOf[NumberFormat]

    val function = { (input: Double) =>
      clonedFormatter.synchronized {
        clonedFormatter.format(input)
      }
    }

    function
  }

  override def parse(cellKey: CellKey, locale: Locale, input: String): Either[CellParsingError, DoubleCell] = {

    // TODO: Refactor, make the numberFormat somehow client code configurable.
    // NOTE: This creates a local instance of NumberFormat to workaround
    // thread safety problem http://stackoverflow.com/a/1285353/1148030
    val numberFormat: NumberFormat = NumberFormat.getInstance(locale)

    try {

      val position = new ParsePosition(0)

      val trimmedInput = input.trim

      val number = numberFormat.parse(trimmedInput, position)

      if (position.getIndex() != trimmedInput.size) {
        Left(CellParsingError(s"Expected decimal number (double precision), but input '$input' could not be fully parsed with locale '$locale'."))
      } else {

        val doubleValue = number.doubleValue()

        Right(DoubleCell(cellKey, doubleValue)(toSynchronizedFormatter(numberFormat)))
      }

    } catch {
      case e: ParseException =>
        Left(CellParsingError(s"Expected a decimal number (double precision), but input '$input' could not be parsed with locale '$locale'"))
    }
  }
}

