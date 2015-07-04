package fi.pelam.csv

import java.util.Locale

import enumeratum.{Enum, EnumEntry}

sealed abstract class TestColType extends EnumEntry

object TestColType extends Enum[TestColType] {

  val values = findValues

  case object CommentCol extends TestColType
  case object Qualifications extends TestColType
  case object RowType extends TestColType
  case object PrevWeek extends TestColType
  case object WorkerId extends TestColType
  case object IntParam1 extends TestColType
  case object Salary extends TestColType
  case object ThisWeek extends TestColType
  case object BoolParam1 extends TestColType
  case object IntParam2 extends TestColType

  val translations = Map(Locale.ROOT -> namesToValuesMap,
    Locales.localeFi -> Map(
      "KommenttiSarake" -> CommentCol,
      "Tyypit" -> Qualifications,
      "RiviTyyppi" -> RowType,
      "EdellinenViikko" -> PrevWeek,
      "TyöntekijäId" -> WorkerId,
      "IntParam1" -> IntParam1,
      "Palkka" -> Salary,
      "KuluvaViikko" -> ThisWeek,
      "BoolParam1" -> BoolParam1,
      "IntParam2" -> IntParam2))

}
