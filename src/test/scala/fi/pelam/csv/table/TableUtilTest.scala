package fi.pelam.csv.table

import fi.pelam.csv.table.TableTest._
import fi.pelam.csv.table.TableUtil._
import org.junit.Assert._
import org.junit.Test

class TableUtilTest {

  @Test
  def testRenumberDown: Unit = {
    assertEquals("List(StringCell with value '4b' at Row 2, Column B (1), " +
      "StringCell with value '3c' at Row 3, Column B (1))",
      renumberedAsRows(List(cell4b, cell3c), (cell2b.cellKey, cell3c.cellKey)).toList.toString())
  }

  @Test
  def testRenumberDown2Cols: Unit = {
    assertEquals("List(StringCell with value '4b' at Row 2, Column B (1), " +
      "StringCell with value '3c' at Row 2, Column C (2), " +
      "StringCell with value '4c' at Row 3, Column B (1)" +
      ")",
      renumberedAsRows(List(cell4b, cell3c, cell4c), (cell2b.cellKey, cell3d.cellKey)).toList.toString())
  }
}