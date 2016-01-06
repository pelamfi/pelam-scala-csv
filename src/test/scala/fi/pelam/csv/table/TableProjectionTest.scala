package fi.pelam.csv.table

import fi.pelam.csv.cell._
import org.junit.Assert._
import org.junit.Test

import scala.collection.SortedSet

class TableProjectionTest {

  import TableTest._

  val emptyProj: TableProjection[TestRowType, TestColType, SimpleMetadata] = testTable.projection

  val withRows = emptyProj.withRowTypes(TestRowType.Worker, TestRowType.Day)

  val projection = withRows.withColTypes(TestColType.PrevWeek, TestColType.Qualifications)

  @Test
  def testProjected: Unit = {
    val result = projection.projected

    assertEquals("columns:Qualifications,PrevWeek,PrevWeek,\n" +
      "Row 1/Worker:2b,2c,,\n" +
      "Row 2/Worker:3b,,,\n" +
      "Row 3/Day:4b,,,\n", result.toString())
  }

  @Test
  def testProjectionByTypes: Unit = {
    assertEquals(SortedSet(RowKey(1), RowKey(2), RowKey(3)), projection.rows)
    assertEquals(SortedSet(ColKey(1), ColKey(2), ColKey(3)), projection.cols)
  }

  @Test
  def testProjectionInverse: Unit = {
    val invProj = projection.inverse
    val inv = invProj.projected

    assertEquals("columns:,ThisWeek,,CommentCol,\n" +
      "Row 1/CommentRow:,,,,\n" +
      "Row 2/:5a-untyped,,5f-untyped,,\n" +
      "Row 3/CommentRow:,,,,\n", inv.toString())
  }
}