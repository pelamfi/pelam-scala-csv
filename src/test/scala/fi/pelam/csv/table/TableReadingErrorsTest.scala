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

import fi.pelam.csv.cell.{CellKey, StringCell}
import org.junit.Test
import org.junit.Assert._

class TableReadingErrorsTest {
  import TableReadingErrorsTest._

  @Test
  def testToString: Unit = {
    assertEquals("No reading has been attempted yet. Last stage was -1.", TableReadingErrors.initial.toString)
    assertEquals("Some error occured. The error is related to the StringCell with value 'foocell' at Row 2, Column C (2). Last stage was 100.", TableReadingErrors(100, IndexedSeq(errorFoo)).toString)
    assertEquals("2 errors in stage 100. The first error is: Some error occured. The error is related to the StringCell with value 'foocell' at Row 2, Column C (2).", TableReadingErrors(100, IndexedSeq(errorFoo, errorBar)).toString)
  }
}

object TableReadingErrorsTest {
  val errorFoo = TableReadingError("Some error occured.", Some(StringCell(CellKey(1,2),"foocell")))
  val errorBar = TableReadingError("Another error occured.", Some(StringCell(CellKey(3,4),"barcell")))

}