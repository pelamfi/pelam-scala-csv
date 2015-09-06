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

import fi.pelam.csv.cell.{CellKey, IntegerCell, StringCell}
import org.junit.Assert._
import org.junit.Test

class TableReadingErrorTest {

  import TableReadingErrorTest._

  @Test
  def testToString: Unit = {
    assertEquals("Some error occured. The error is related to the StringCell with value 'foocell' at Row 2, Column C (2).", errorFoo.toString)
    assertEquals("Another error occured. The error is related to the IntegerCell with value '123' at Row 3, Column D (3).", errorBar.toString)
  }

  @Test
  def testAddedDetailsCellDefined: Unit = {

    // This should have no effect
    val alreadySpecifiedCell = errorFoo.relatedCellAdded(intCell)

    assertEquals("Some error occured. " +
      "The error is related to the StringCell with value 'foocell' at Row 2, Column C (2).", alreadySpecifiedCell.toString)
  }

  @Test
  def testAddedDetailsMessage: Unit = {

    // This should have no effect
    val alreadySpecifiedCell = errorFoo.messageAppended("Appended.")

    assertEquals("Some error occured. Appended. " +
      "The error is related to the StringCell with value 'foocell' at Row 2, Column C (2).", alreadySpecifiedCell.toString)
  }

  @Test
  def testAddedDetailsCell: Unit = {

    val modifiedError = TableReadingError("Error originally without Cell.").relatedCellAdded(intCell)

    assertEquals("Error originally without Cell. " +
      "The error is related to the IntegerCell with value '123' at Row 3, Column D (3).", modifiedError.toString)
  }

}

object TableReadingErrorTest {
  val stringCell = StringCell(CellKey(1, 2), "foocell")
  val errorFoo = TableReadingError("Some error occured.", Some(stringCell))
  val intCell = IntegerCell(CellKey(2, 3), 123)
  val errorBar = TableReadingError("Another error occured.", Some(intCell))

}