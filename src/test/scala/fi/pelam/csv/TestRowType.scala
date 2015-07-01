package fi.pelam.csv

sealed abstract class TestRowType {

}

object TestRowType {
  case object ColumnHeader extends TestRowType
  case object CommentRow extends TestRowType
  case object Worker extends TestRowType
  case object Day extends TestRowType
}
