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

/**
 * Captures errors that happen inside [[TableReader]]. This class is ordered in
 * increasing success orderd. This ordering is used in format detection
 * heuristics to pick the solution that produces best (least badness) results.
 *
 * @param stageNumber The number of stage reached in TableReader. Before any stages are run this is zero. After the first stage this is 1 etc.
 * @param errors List of errors. All errors are from same stage, because TableReader stops after first stage that produces errors.
 */
final case class TableReadingErrors(stageNumber: Int = 0, errors: IndexedSeq[TableReadingError] = IndexedSeq()) extends Ordered[TableReadingErrors] {

  import TableReadingErrors._

  def addError(moreErrors: TraversableOnce[TableReadingError]): TableReadingErrors = {
    copy(errors = errors ++ moreErrors.toIndexedSeq)
  }

  def addError(error: TableReadingError): TableReadingErrors = copy(errors = errors :+ error)

  def noErrors = errors.isEmpty

  // Order these status objects in increasing goodness.
  // The later the the stage reached, the better the situation.
  // If the stage reached is same, least errors is best.
  private def orderingTuple: OrderingTuple = (stageNumber, -errors.size)

  override def compare(that: TableReadingErrors): Int = {
    tupleOrdering.compare(this.orderingTuple, that.orderingTuple)
  }

  override def toString = {
    if (errors.size == 0) {
      s"No errors (stage $stageNumber)"
    } else if (errors.size == 1) {
      s"Error occured in reading table:\n  ${errors(0)}\n  Stage number is $stageNumber."
    } else {
      s"${errors.size} errors in stage number $stageNumber.\n  The first error is:\n  ${errors(0)}"
    }
  }
}

object TableReadingErrors {

  /**
   * Special initial value which is worse than any result from any real stage.
   * This value can be used as an initial value in format detection heuristics.
   */
  val initialValue = TableReadingErrors(0, IndexedSeq(TableReadingError("No reading was attempted.")))

  private type OrderingTuple = (Int, Int)

  private val tupleOrdering = implicitly(Ordering[OrderingTuple])
}
