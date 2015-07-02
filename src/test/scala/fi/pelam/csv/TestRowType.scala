package fi.pelam.csv

import enumeratum.{Enum, EnumEntry}

sealed abstract class TestRowType extends EnumEntry{

}

object TestRowType extends Enum[TestRowType] {

  val values = findValues

  case object ColumnHeader extends TestRowType
  case object CommentRow extends TestRowType
  case object Worker extends TestRowType
  case object Day extends TestRowType
}
