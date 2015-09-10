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

class DetectingTableReaderExample {

  @Test
  def testFromCodeExample() = {
    import TableReaderConfig._
    import fi.pelam.csv.cell._

    val validColTypes = Set("header", "model", "price")

    // Setup a DetectingTableReader which will try combinations of CSV formatting types
    // to understand the data.
    val reader = DetectingTableReader[String, String](

      tableReaderMaker = { (metadata) => new TableReader(

        // An implicit from the object TableReaderConfig converts the string
        // to a function providing streams.
        openStream =
          "header;model;price\n" +
          "data;300D;1,234.0\n" +
          "data;SLS AMG;234,567.89",

        // Make correct metadata end up in the final Table
        tableMetadata = metadata,

        // First column specifies row types
        rowTyper = makeRowTyper({
          case (CellKey(_, 0), rowType) => rowType
        }),

        // Column type is specified by the first row.
        // Type names are checked and error is generated for unknown
        // column types by errorOnUndefinedCol.
        // This strictness is what enables the correct detection of CSV format.
        colTyper = errorOnUndefinedCol(makeColTyper({
          case (CellKey(0, _), colType) if validColTypes.contains(colType) => colType
        })),

        cellUpgrader = makeCellUpgrader({
          case CellType("data", "price") => DoubleCell.parserForLocale(metadata.dataLocale)
        }))
      }
    )

    val table = reader.readOrThrow()

    assertEquals(List("300D", "SLS AMG"), table.getSingleCol("data", "model").map(_.value).toList)
    assertEquals(List(1234.0, 234567.89), table.getSingleCol("data", "price").map(_.value).toList)

  }

}
