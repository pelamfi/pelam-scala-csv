package fi.pelam.csv

sealed abstract class TestColType

object TestColType {

  case object CommentCol extends TestColType

  case object Qualifications extends TestColType

  case object RowHeader extends TestColType

  case object PrevWeek extends TestColType

  case object WorkerId extends TestColType

  case object IntParam1 extends TestColType

  case object Salary extends TestColType

  case object ThisWeek extends TestColType

  case object BoolParam1 extends TestColType

  case object IntParam2 extends TestColType
}
