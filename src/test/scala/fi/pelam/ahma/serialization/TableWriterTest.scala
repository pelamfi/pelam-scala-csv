package fi.pelam.ahma.serialization

import java.io.ByteArrayOutputStream

import com.google.common.io.Resources
import org.junit.Assert._
import org.junit.Test

class TableWriterTest {

  import TableTest._

  val outputStream = new ByteArrayOutputStream()

  @Test
  def testWrite: Unit = {
    val table = TableTest.makeTable()

    table.setCell(foo)
    table.setCell(bar)

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.charset)

    assertEquals(",,,,,\n,foo,,,,\n,bar,,,,\n,,,,,\n,,,,,\n", written)
  }

  @Test
  def testLoopback: Unit = {
    val file = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

    val table = new TableReader(file, Map()).read()

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.charset)

    // Added \n because table writer ensures there is traling newline
    val reference = new String(file.read(), table.charset) + "\n"

    assertEquals(reference, written)
  }
}
