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

/**
 * Type for row numbers used in parsed CSV data.
 *
 * This corresponds to zero based row number in CSV text file.
 *
 * @constructor Construct a row key object for given zero based row index integer.
 *
 * @param index zero based row number (row index).
 */
case class RowKey(index: Int) extends AxisKey[RowKey] {

  override def toString(): String = {
    s"Row ${index + 1}"
  }

  override def updated(newIndex: Int) = copy(index = newIndex)

  override def withOffset(offset: Int) = copy(index = index + offset)
}
