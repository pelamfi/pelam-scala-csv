package fi.pelam.csv.table

import java.io.OutputStreamWriter

class TableWriter[RT, CT, M <: TableMetadata](table: Table[RT, CT, M]) {

  def write(output: java.io.OutputStream) = {

    val writer = new OutputStreamWriter(output, table.charset)

    val csvWriter = new CsvWriter(writer, table.csvSeparator)

    val cells = table.getCells()

    csvWriter.write(cells)

    // Add the final line end
    csvWriter.goToNextRow()

    // TODO: Make sure streams are closed if exception is thrown
    writer.close()

    output.close()
  }

}
