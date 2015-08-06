package fi.pelam.csv.cell

import java.text.NumberFormat
import java.util.Locale

import org.junit.Assert._
import org.junit.Test

class IntegerCellTest {

  val localeFi: Locale = Locale.forLanguageTag("FI")

  @Test
  def testFromStringError: Unit = {
    assertEquals(Left(CellParsingError(s"Expected integer, but input '12,000.0' " +
      s"could not be fully parsed with locale 'fi'.")),
      IntegerCell.parse(CellKey(0, 0), localeFi, "12,000.0"))
  }

  @Test
  def javaNumberFormatTest: Unit = {
    assertEquals(
      "Thousand separator is non breaking space",
      "12\u00A0000",
      NumberFormat.getInstance(localeFi).format(12000))

    assertEquals("Non breaking space as thousand separator is parsed",
      12000L,
      NumberFormat.getInstance(localeFi).parse("12\u00A0000"))
  }

  @Test
  def testFromStringFi: Unit = {
    assertEquals("12\u00A0000",
      IntegerCell.parse(CellKey(0, 0), localeFi, "12000,0").right.get.serializedString)
  }
}
