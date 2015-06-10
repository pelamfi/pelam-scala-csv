package fi.pelam.csv

abstract class CellFactory {
  def fromString(cellKey: CellKey, locale: java.util.Locale, input: String): Either[TableReadingError, Cell]
}
