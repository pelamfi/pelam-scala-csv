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

import fi.pelam.csv.cell.{Cell, CellParsingError}
import fi.pelam.csv.stream.CsvReaderError

/**
 * Various phases in [[TableReader]] produce these when building a Table object from input fails.
 * [[fi.pelam.csv.cell.CellParsingError CellParsingErrors]] errors are converted to these errors in [[TableReader]].
 *
 * @constructor See alternate constructors on the companion object.
 */
case class TableReadingError(msg: String, cell: Option[Cell] = None) {

  override def toString() = {
    if (cell.isDefined) {
      msg + " The error is related to the " + cell.get + "."
    } else {
      msg
    }
  }

  def messageAppended(appended: String) = {
    copy(msg = msg + " " + appended)
  }

  def relatedCellAdded(specifiedCell: Cell): TableReadingError = {
    copy(cell = Some(cell.getOrElse(specifiedCell)))
  }

}

object TableReadingError {

  def apply(innerError: CellParsingError, cell: Cell, cellType: CellType[_, _]): TableReadingError = {
    TableReadingError(s"$innerError $cellType", Some(cell))
  }

  def apply(innerError: CsvReaderError): TableReadingError = {
    TableReadingError(innerError.toString, None)
  }


}
