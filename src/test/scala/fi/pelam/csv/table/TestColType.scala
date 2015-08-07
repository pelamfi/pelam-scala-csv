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

sealed abstract class TestColType extends EnumEntry

/**
 * Sample column type enumeration for use in tests of [[fi.pelam.csv.table.Table]]
 * related classes.
 */
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
