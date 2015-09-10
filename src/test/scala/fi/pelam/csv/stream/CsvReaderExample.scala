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

package fi.pelam.csv.stream

import org.junit.Test

class CsvReaderExample {

  @Test
  def testParseExample: Unit = {
    import fi.pelam.csv.cell._

    val csvData =
      "apple,0.99,3\n" +
      "orange,1.25,2\n" +
      "banana,0.80,4\n"

    val pickedCol = ColKey(1)

    for (cell <- new CsvReader(csvData).throwOnError; if cell.colKey == pickedCol) {
      println(cell)
    }

  }


}
