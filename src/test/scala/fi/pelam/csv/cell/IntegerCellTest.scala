package fi.pelam.csv.cell

import java.util.Locale

import fi.pelam.csv.table.TableReadingError
import org.junit.Assert._
import org.junit.Test

class IntegerCellTest {

  val localeFi: Locale = Locale.forLanguageTag("FI")

  @Test
  def testFromString: Unit = {
    assertEquals(Left(CellParsingError(s"Expected integer, but input '12,000.0' " +
      s"could not be fully parsed with locale 'fi'.")),
      IntegerCell.parse(CellKey(0, 0), localeFi, "12,000.0"));
  }
}
