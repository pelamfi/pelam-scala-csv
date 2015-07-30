package fi.pelam.csv.table

import java.util.Locale

import enumeratum.{Enum, EnumEntry}

sealed abstract class TestRowType extends EnumEntry

/**
 * Sample row type enumeration for use in tests of [[fi.pelam.csv.table.Table]]
 * related classes.
 */
object TestRowType extends Enum[TestRowType] {

  val values = findValues

  case object ColumnHeader extends TestRowType

  case object CommentRow extends TestRowType

  case object Worker extends TestRowType

  case object Day extends TestRowType

  case object WeekDay extends TestRowType

  case object Holiday extends TestRowType

  val translations = Map(Locale.ROOT -> namesToValuesMap,
    Locales.localeFi -> Map(
      "SarakeOtsikko" -> ColumnHeader,
      "KommenttiRivi" -> CommentRow,
      "Työntekijä" -> Worker,
      "Päivä" -> Day,
      "Viikonpäivä" -> WeekDay,
      "Pyhäpäivä" -> Holiday))

}

