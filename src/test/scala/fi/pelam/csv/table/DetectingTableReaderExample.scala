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
import fi.pelam.csv.util.TableReaderImplicits
import org.junit.Test
import org.junit.Assert._
import java.nio.charset.{StandardCharsets, Charset}

import fi.pelam.csv.CsvConstants
import org.junit.Test
import org.junit.Assert._
import java.io.ByteArrayInputStream
import java.util.Locale

import com.google.common.io.{ByteSource, Resources}
import fi.pelam.csv.cell._
import fi.pelam.csv.table.Locales.localeFi
import fi.pelam.csv.table.TestColType._
import fi.pelam.csv.table.TestRowType._
import org.junit.Assert._
import org.junit.Test
import fi.pelam.csv.cell._

class DetectingTableReaderExample {
  import TableReaderTest._
  import DetectingTableReaderTest._
  import fi.pelam.csv.util.TableReaderImplicits._

  @Test
  def testFromCodeExample() = {
    // TODO: Add this as a code sample

    val validColTypes = Set("header", "name", "number")

    val reader = DetectingTableReader[String, String](

      tableReaderMaker = { (metadata) => new TableReader(
        openStream = "header;name;number\n" +
          "data;foo;1,234.0\n" +
          "data;bar;1,234,567.89",

        tableMetadata = metadata,

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
          case CellType("data", "number") => DoubleCell
        },
        metadata.dataLocale
        ))
      }
    )

    val table = reader.readOrThrow()

    assertEquals(List("foo", "bar"), table.getSingleCol("data", "name").map(_.value).toList)
    assertEquals(List(1234, 1234567.89), table.getSingleCol("data", "number").map(_.value).toList)

  }

}
