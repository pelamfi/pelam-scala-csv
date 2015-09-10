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

    // Create a TableReader that parses a small bit of CSV data in which the
    // column types are defined on the first row.
    val reader = new TableReader[String, String, SimpleMetadata](

      // An implicit from the object TableReaderConfig converts the string
      // to a function providing streams.
      openStream =
        "product,price,number\n" +
        "apple,0.99,3\n" +
        "orange,1.25,2\n" +
        "banana,0.80,4\n",

      // The first row is the header, the rest are data.
      rowTyper = makeRowTyper({
        case (CellKey(0, _), _) => "header"
        case _ => "data"
      }),

      // First row defines column types.
      colTyper = makeColTyper({
        case (CellKey(0, _), colType) => colType
      }),

      // Convert cells on the "data" rows in the "number" column to integer cells.
      cellUpgrader = makeCellUpgrader({
        case CellType("data", "number") => IntegerCell.defaultParser
        case CellType("data", "price") => DoubleCell.defaultParser
      }))

    // Read the data
    val table = reader.readOrThrow()

    assertEquals(List("apple", "orange", "banana"), table.getSingleCol("data", "product").map(_.value).toList)
    assertEquals(List(0.99, 1.25, 0.8), table.getSingleCol("data", "price").map(_.value).toList)
  }


}
