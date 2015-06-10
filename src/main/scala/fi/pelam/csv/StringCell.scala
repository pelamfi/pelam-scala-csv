package fi.pelam.csv

import java.util.Locale

case class StringCell(override val cellKey: CellKey,
  override val serializedString: String)
  extends Cell {

  override def toString() = s"Cell containing '$serializedString' at $cellKey"
}

object StringCell extends CellFactory {

  override def fromString(cellKey: CellKey, locale: Locale, input: String): Either[TableReadingError, StringCell] = {
    Right(StringCell(cellKey, input))
  }
}
