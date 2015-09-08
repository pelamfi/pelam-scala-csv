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

package fi.pelam.csv.cell

import java.text.NumberFormat
import java.util.Locale

import org.junit.Assert._
import org.junit.Test

class IntegerCellTest {

  val localeFi: Locale = Locale.forLanguageTag("FI")

  @Test
  def testFromStringError: Unit = {
    assertEquals(Left(CellParsingError(s"Expected integer, but input '12,000.0' " +
      s"could not be fully parsed. Used locale 'fi'.")),
      IntegerCell.parserForLocale(localeFi)(CellKey(0, 0), "12,000.0"))
  }

  @Test
  def javaNumberFormatTest: Unit = {
    assertEquals(
      "Thousand separator is non breaking space",
      "12\u00A0000",
      NumberFormat.getInstance(localeFi).format(12000))

    assertEquals("Non breaking space as thousand separator is parsed",
      12000L,
      NumberFormat.getInstance(localeFi).parse("12\u00A0000"))
  }

  @Test
  def testFromStringFi: Unit = {
    assertEquals("12\u00A0000",
      IntegerCell.parserForLocale(localeFi)(CellKey(0, 0), "12000,0").right.get.serializedString)
  }
}
