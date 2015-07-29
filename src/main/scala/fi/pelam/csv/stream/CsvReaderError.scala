package fi.pelam.csv.stream

import fi.pelam.csv.cell.CellKey

/**
 * [[fi.pelam.csv.stream.CsvReader CsvReader]] produces these in various situations
 * when it can't understand the input CSV data.
 *
 * @param message is a descriptive message.
 * @param at last known coordinates in CSV data (or corresponding spreadsheet).
 */
case class CsvReaderError(message: String, at: CellKey) {
  override def toString = s"Error parsing CSV at $at: $message"
}
