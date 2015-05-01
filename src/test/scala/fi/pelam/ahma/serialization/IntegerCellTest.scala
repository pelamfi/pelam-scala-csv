package fi.pelam.ahma.serialization

import fi.pelam.ahma.localization.AhmaLocalization
import org.junit.Assert._
import org.junit.Test

class IntegerCellTest {
  @Test
  def testFromString: Unit = {
    assertEquals(Left(TableReadingError(s"Expected integer, but input '12,000.0' " +
      s"could not be fully parsed with locale fi at Row 1, Column A (0)")),
      IntegerCell.fromString(CellKey(0, 0), AhmaLocalization.localeFi, "12,000.0"));
  }
}
