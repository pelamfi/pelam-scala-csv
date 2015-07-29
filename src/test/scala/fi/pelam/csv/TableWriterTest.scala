package fi.pelam.csv

import java.io.ByteArrayOutputStream

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.table.{TableWriter, TableReader}
import org.junit.Assert._
import org.junit.Test

object TableWriterTest {

  val testFileName = "csv–file-for-loading"

  lazy val testFile = Resources.asByteSource(Resources.getResource(testFileName))

  val testFileCharset = Charsets.UTF_8

  // Added \n because table writer ensures there is traling newline
  lazy val testFileContent = readTestFile(testFileName) + "\n"

  def readTestFile(resource: String) = {
    val testFile = Resources.asByteSource(Resources.getResource(resource))
    new String(testFile.read(), testFileCharset)
  }

}

class TableWriterTest {

  import TableTest._
  import TableWriterTest._

  val outputStream = new ByteArrayOutputStream()

  @Test
  def testWrite: Unit = {
    val table = TableTest.makeTable().updatedCells(foo, bar)

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.charset)

    assertEquals(",,,,,\n,foo,,,,\n,bar,,,,\n,,,,,\n,,,,,\n", written)
  }

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }

  @Test
  def testLoopback: Unit = {
    val table = new TableReader[TestRowType, TestColType](testFile).read()

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.charset)

    assertEquals(testFileContent, written)
  }
}
