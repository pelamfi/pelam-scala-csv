package fi.pelam.csv

import java.util.Locale

/**
 * The most basic subtype of [[Cell]].
 *
 * Just contains the raw data from a position in a CSV text file.
 *
 * However note that any possible quoting is removed and no separator or CSV line ending
 * characters are included.
 *
 * The lower level CSV parser API [[CsvReader]] only produces these.
 *
 * @param cellKey the location of the cell in the CSV file.
 * @param serializedString simply the string from the CSV file.
 */
case class StringCell(override val cellKey: CellKey,
  override val serializedString: String)
  extends Cell {

  override def toString() = s"Cell containing '$serializedString' at $cellKey"
}

object StringCell extends CellUpgrade {

  override def fromString(cellKey: CellKey, locale: Locale, input: String): Either[TableReadingError, StringCell] = {
    Right(StringCell(cellKey, input))
  }
}
