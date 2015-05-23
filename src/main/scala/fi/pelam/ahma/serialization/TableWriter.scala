package fi.pelam.ahma.serialization

import java.io.OutputStreamWriter

class TableWriter(table: Table) {

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
