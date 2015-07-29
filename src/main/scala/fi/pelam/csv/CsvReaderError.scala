package fi.pelam.csv

import fi.pelam.csv.cell.CellKey

/**
 * [[CsvReader]] produces these in various situations when it can't understand the input CSV data.
 *
 * @param message is a descriptive message.
 * @param at last known coordinates in CSV data (or corresponding spreadsheet).
 */
case class CsvReaderError(message: String, at: CellKey) {
  override def toString = s"Error parsing CSV at $at: $message"
}
