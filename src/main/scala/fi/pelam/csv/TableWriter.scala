package fi.pelam.csv

import java.io.OutputStreamWriter

import fi.pelam.csv.stream.CsvWriter

class TableWriter[RT, CT](table: Table[RT, CT]) {

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
