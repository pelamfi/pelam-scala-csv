package fi.pelam.csv

import java.io.OutputStreamWriter

class TableWriter[RT, CT, M <: TableMetadata](table: Table[RT, CT, M]) {

  def write(output: java.io.OutputStream) = {

    val writer = new OutputStreamWriter(output, table.charset)

    val csvWriter = new CsvWriter(writer, table.csvSeparator)

    val cells = table.getCells()

    csvWriter.write(cells)

    csvWriter.goToNextRow()

    writer.close()

    output.close()
  }

}
