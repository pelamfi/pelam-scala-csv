/*
 * This file is part of pelam-scala-csv
 *
 * Copyright © Peter Lamberg 2015 (pelam-scala-csv@pelam.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

