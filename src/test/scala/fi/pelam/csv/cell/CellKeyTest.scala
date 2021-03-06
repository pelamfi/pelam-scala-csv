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

import org.junit.Assert._
import org.junit.Test

class CellKeyTest {
  @Test
  def testToString: Unit = {

    assertEquals("Row 1, Column A (0)", CellKey(0, 0).toString)
    assertEquals("Row 1, Column B (1)", CellKey(0, 1).toString)
    assertEquals("Row 1, Column Y (24)", CellKey(0, 24).toString)
    assertEquals("Row 1, Column Z (25)", CellKey(0, 25).toString)
    assertEquals("Row 1, Column AA (26)", CellKey(0, 26).toString)
    assertEquals("Row 1, Column AB (27)", CellKey(0, 26 + 1).toString)
    assertEquals("Row 1, Column AZ (51)", CellKey(0, 26 * 2 - 1).toString)
    assertEquals("Row 1, Column BA (52)", CellKey(0, 26 * 2).toString)
    assertEquals("Row 1, Column BB (53)", CellKey(0, 26 * 2 + 1).toString)
    assertEquals("Row 1, Column BZ (77)", CellKey(0, 26 * 3 - 1).toString)
    assertEquals("Row 1, Column CA (78)", CellKey(0, 26 * 3).toString)
    assertEquals("Row 1, Column CB (79)", CellKey(0, 26 * 3 + 1).toString)
    assertEquals("Row 1, Column ZZ (701)", CellKey(0, 26 * 27 - 1).toString)
    assertEquals("Row 1, Column AAA (702)", CellKey(0, 26 * 27).toString)
    assertEquals("Row 1, Column AAB (703)", CellKey(0, 26 * 27 + 1).toString)

  }
}
