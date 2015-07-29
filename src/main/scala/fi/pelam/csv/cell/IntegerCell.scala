package fi.pelam.csv.cell

import java.text.{NumberFormat, ParseException, ParsePosition}
import java.util.Locale

import fi.pelam.csv.table.{CellUpgrade, TableReader, TableReadingError}

/**
 * Basically a sample implementation of a more specialised subtype of [[fi.pelam.csv.cell.Cell]].
 *
 * It is expected that any nontrivial client will want to specify its own subtypes
 * of [[fi.pelam.csv.cell.Cell]].
 *
 * The IntegerCell class it self is quite simple, but the companion object is more
 * interesting as it implements the [[CellUpgrade]] trait and acts as a factory
 * which produces IntegerCell instances (or errors if parsing fails) from String data.
 *
 * @constructor Create a new instance of CSV cell holding an integer. Also see companion object.
 *
 * @param cellKey the location of this cell in a CSV table.
 * @param numberFormat Java number format used for integer data in this cell in CSV.
 * @param value is the integer stored in CSV.
 */
case class IntegerCell(override val cellKey: CellKey,
  val numberFormat: NumberFormat, val value: Int)
  extends Cell {

  def serializedString: String = {
    numberFormat.format(value)
  }

  override def toString() = s"Cell containing '$serializedString' at $cellKey"
}

/**
 * The IntegerCell class it self is quite simple, but this companion object is more
 * interesting as it implements the [[CellUpgrade]] trait and acts as a factory
 * which produces IntegerCell instances (or errors if parsing fails) from String data.
 *
 * This companion object can be used as an argument to [[TableReader.cellTypes]]
 * to specify which cells should be interpreted as containing integers.
 */
// TODO: Is there a scaladoc way to refer to cellTypes in TableReader?
object IntegerCell extends CellUpgrade {

  override def fromString(cellKey: CellKey, locale: Locale, input: String): Either[TableReadingError, IntegerCell] = {

    // TODO: Refactor, make the numberFormat somehow client code configurable.
    val numberFormat: java.text.NumberFormat = NumberFormat.getInstance(locale)

    try {

      val position = new ParsePosition(0)

      val trimmedInput = input.trim

      val number = numberFormat.parse(trimmedInput, position)

      if (position.getIndex() != trimmedInput.size) {
        Left(TableReadingError(s"Expected integer, but input '$input' could not be fully parsed with locale '$locale'."))
      } else {

        val intValue = number.intValue()

        if (intValue == number) {
          Right(IntegerCell(cellKey, numberFormat, intValue))
        } else {
          Left(TableReadingError(s"Expected integer, but value '$input' is decimal"))
        }

      }

    } catch {
      case e: ParseException =>
        Left(TableReadingError(s"Expected integer, but input '$input' could not be parsed with locale '$locale'"))
    }
  }
}


