package fi.pelam.ahma.serialization

import java.text.NumberFormat
import java.util.Locale

import scala.reflect.macros.ParseException

case class IntegerCell(override val cellKey: CellKey,
  val numberFormat: NumberFormat, val value: Int)
  extends Cell {

  def serializedString: String = {
    numberFormat.format(value)
  }

  override def toString() = s"Cell containing '$serializedString' at $cellKey"
}

object IntegerCell extends CellFactory {

  override def fromString(cellKey: CellKey, locale: Locale, input: String): Either[TableReadingError, IntegerCell] = {

    val numberFormat = NumberFormat.getInstance(locale)

    try {

      val number = numberFormat.parse(input)

      val intValue = number.intValue()

      if (intValue == number) {
        Right(IntegerCell(cellKey, numberFormat, intValue))
      } else {
        Left(TableReadingError(s"Expected integer, but value '$input' is decimal at $cellKey"))
      }

    } catch {
      case e: ParseException =>
        Left(TableReadingError(s"Expected integer, but value '$input' could not be parsed with locale $locale at $cellKey"))
    }

  }
}

