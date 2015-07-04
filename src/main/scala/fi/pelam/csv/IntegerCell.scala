package fi.pelam.csv

import java.text.{ParseException, NumberFormat, ParsePosition}
import java.util.Locale

case class IntegerCell(override val cellKey: CellKey,
  val numberFormat: NumberFormat, val value: Int)
  extends Cell {

  def serializedString: String = {
    numberFormat.format(value)
  }

  override def toString() = s"Cell containing '$serializedString' at $cellKey"
}

object IntegerCell extends CellUpgrade {

  override def fromString(cellKey: CellKey, locale: Locale, input: String): Either[TableReadingError, IntegerCell] = {

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


