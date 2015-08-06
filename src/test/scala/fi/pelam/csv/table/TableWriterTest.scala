package fi.pelam.csv.table

import java.io.ByteArrayOutputStream

import com.google.common.base.Charsets
import com.google.common.io.{ByteSource, Resources}
import org.junit.Assert._
import org.junit.Test

// TODO: Move object after class in file
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
    val table = TableTest.table.updatedCells(foo, bar)

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.metadata.charset)

    assertEquals(",,,,,\n,foo,,,,\n,bar,,,,\n,,,,,\n,,,,,\n", written)
  }

  implicit def opener(byteSource: ByteSource): () => java.io.InputStream = {
    () => byteSource.openStream()
  }

  @Test
  def testLoopback: Unit = {
    val (table, errors) = new TableReader[TestRowType, TestColType, SimpleTableMetadata](testFile).read()

    assertTrue(errors.toString, errors.noErrors)

    val writer = new TableWriter(table)

    writer.write(outputStream)

    val written = new String(outputStream.toByteArray(), table.metadata.charset)

    assertEquals(testFileContent, written)
  }
}