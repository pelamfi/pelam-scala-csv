package fi.pelam.ahma.serialization

import java.io.ByteArrayOutputStream

import com.google.common.base.Charsets
import com.google.common.io.Resources
import org.junit.Assert._
import org.junit.Test

object TableWriterTest {
  val testFile = Resources.asByteSource(Resources.getResource("csv–file-for-loading"))

  val testFileCharset = Charsets.UTF_8

  // Added \n because table writer ensures there is traling newline
  val testFileContent = new String(testFile.read(), testFileCharset) + "\n"

}

class TableWriterTest {

  import TableTest._
  import TableWriterTest._

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
    val table = new TableReader(testFile, Map()).read()

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.charset)

    assertEquals(testFileContent, written)
  }
}
