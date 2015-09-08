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

import org.junit.Assert._
import org.junit.Test

class TableReaderExample {

  @Test
  def testCodeExample() = {
    import TableReaderConfig._
    import fi.pelam.csv.cell._

    val reader = new TableReader[String, String, SimpleMetadata](
      openStream = "name,number\n" +
        "foo,1\n" +
        "bar,2",

      rowTyper = makeRowTyper({
        case (CellKey(0, _), _) => "header"
        case _ => "data"
      }),

      colTyper = makeColTyper({
        case (CellKey(_, 0), _) => "name"
        case (CellKey(_, 1), _) => "number"
      }),

      cellUpgrader = makeCellUpgrader({
        case CellType("data", "number") => IntegerCell.defaultParser
      }))

    val table = reader.readOrThrow()

    assertEquals(List("foo", "bar"), table.getSingleCol("data", "name").map(_.value).toList)
    assertEquals(List(1, 2), table.getSingleCol("data", "number").map(_.value).toList)
  }


}
