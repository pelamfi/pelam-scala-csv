package fi.pelam.csv.cell

import java.util.Locale

/**
 * The most basic subtype of [[fi.pelam.csv.cell.Cell Cell]].
 *
 * This cell type simply contains the raw data from a position in a CSV text file.
 *
 * @note However note that any possible quoting is removed and no separator or CSV line ending
 *       characters are included.
 *
 *       The lower level CSV parser API [[fi.pelam.csv.stream.CsvReader CsvReader]] produces these.
 *
 * @param cellKey the location of the cell in the CSV file.
 * @param serializedString simply the string from the CSV file.
 */
case class StringCell(override val cellKey: CellKey,
  override val serializedString: String)
  extends Cell {

  override def toString() = s"Cell containing '$serializedString' at $cellKey"
}

object StringCell extends CellParser {

  override def parse(cellKey: CellKey, locale: Locale, input: String): Either[CellParsingError, StringCell] = {
    Right(StringCell(cellKey, input))
  }
}
