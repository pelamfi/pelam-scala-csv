package fi.pelam.ahma.serialization

import com.google.common.io.CharStreams
import org.junit.Assert._
import org.junit.Test

class CsvWriterTest {

  val stringBuilder = new java.lang.StringBuilder()

  val charWriter = CharStreams.asWriter(stringBuilder)

  val csvWriter = new CsvWriter(charWriter)

  @Test
  def testWrite: Unit = {
    csvWriter.write(List(StringCell(CellKey(0, 0), "foo"), StringCell(CellKey(0, 0), "bar")))
    assertEquals("foo,bar\n", stringBuilder.toString())
  }
}
