package fi.pelam.csv.table

import java.io.OutputStreamWriter

import fi.pelam.csv.stream.CsvWriter

class TableWriter[RT, CT, M <: TableMetadata](table: Table[RT, CT, M]) {

  def write(output: java.io.OutputStream) = {

    val writer = new OutputStreamWriter(output, table.metadata.charset)

    val csvWriter = new CsvWriter(writer, table.metadata.separator)

    val cells = table.getCells()

    csvWriter.write(cells)

    // Add the final line end
    csvWriter.goToNextRow()

    // TODO: Make sure streams are closed if exception is thrown
    writer.close()

    output.close()
  }

}
