package fi.pelam.csv

abstract class CellUpgrade {
  def fromString(cellKey: CellKey, locale: java.util.Locale, input: String): Either[TableReadingError, Cell]
}
