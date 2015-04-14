package fi.pelam.ahma.serialization

import org.junit.Assert._
import org.junit.Test

class CellKeyTest {
  @Test
  def testToString: Unit = {

    assertEquals("Cell: Row 1 (index 0), Column A (index 0)", CellKey(0, 0).toString)
    assertEquals("Cell: Row 1 (index 0), Column B (index 1)", CellKey(0, 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column Y (index 24)", CellKey(0, 24).toString)
    assertEquals("Cell: Row 1 (index 0), Column Z (index 25)", CellKey(0, 25).toString)
    assertEquals("Cell: Row 1 (index 0), Column AA (index 26)", CellKey(0, 26).toString)
    assertEquals("Cell: Row 1 (index 0), Column AB (index 27)", CellKey(0, 26 + 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column AZ (index 51)", CellKey(0, 26 * 2 - 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column BA (index 52)", CellKey(0, 26 * 2).toString)
    assertEquals("Cell: Row 1 (index 0), Column BB (index 53)", CellKey(0, 26 * 2 + 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column BZ (index 77)", CellKey(0, 26 * 3 - 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column CA (index 78)", CellKey(0, 26 * 3).toString)
    assertEquals("Cell: Row 1 (index 0), Column CB (index 79)", CellKey(0, 26 * 3 + 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column ZZ (index 701)", CellKey(0, 26 * 27 - 1).toString)
    assertEquals("Cell: Row 1 (index 0), Column AAA (index 702)", CellKey(0, 26 * 27).toString)
    assertEquals("Cell: Row 1 (index 0), Column AAB (index 703)", CellKey(0, 26 * 27 + 1).toString)

  }
}
