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

import fi.pelam.csv.cell._
import fi.pelam.csv.util.{LastStageResult, SortedBiMap}

/**
 * This is class is an internal class used to thread state through stages in [[TableReader]].
 */
case class TableReadingState[RT, CT](cells: IndexedSeq[Cell] = IndexedSeq(),
  rowTypes: TableReader.RowTypes[RT] = SortedBiMap[RowKey, RT](),
  colTypes: TableReader.ColTypes[CT] = SortedBiMap[ColKey, CT](),
  errors: TableReadingErrors = TableReadingErrors()) extends LastStageResult[TableReadingState[RT, CT]] {

  override val success: Boolean = errors.noErrors

  def defineRowType(row: RowKey, rowType: RT): TableReadingState[RT, CT] = copy(rowTypes = rowTypes.updated(row, rowType))

  def defineColType(col: ColKey, colType: CT): TableReadingState[RT, CT] = copy(colTypes = colTypes.updated(col, colType))

  def addErrors(moreErrors: Iterator[TableReadingError]): TableReadingState[RT, CT] = copy(errors = errors.addError(moreErrors))

  def addError(error: TableReadingError): TableReadingState[RT, CT] = copy(errors = errors.addError(error))

  override def stageNumberIncremented() = copy(errors = errors.copy(stageNumber + 1))

  override val stageNumber: Int = errors.stageNumber
}
