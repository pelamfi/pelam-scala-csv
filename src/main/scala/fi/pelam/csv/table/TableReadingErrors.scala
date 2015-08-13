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

// TODO: Unit tests and a nice toString
/**
 * Captures errors that happen inside [[TableReader]]. The set of errors
 * is ordered in increasing badness order. This ordering is used in format detection
 * heuristics to pick the solution that produces best (least badness) results.
 *
 * @param stageNumber The number of stage in TableReader. Before any stages are run this is zero. After the first stage this is 1 etc.
 * @param errors List of errors. All errors are from same stage, because TableReader stops after first stage that produces errors.
 */
case class TableReadingErrors(stageNumber: Int = 0, errors: IndexedSeq[TableReadingError] = IndexedSeq()) extends Ordered[TableReadingErrors] {

  import TableReadingErrors._

  def add(moreErrors: TraversableOnce[TableReadingError]): TableReadingErrors = {
    copy(errors = errors ++ moreErrors.toIndexedSeq)
  }

  def add(error: TableReadingError): TableReadingErrors = copy(errors = errors :+ error)

  def noErrors = errors.isEmpty

  // Order errors in increasing goodness. Earlier the stage, the worse the situation.
  // If stage is same, less errors is better.
  private def orderingTuple: OrderingTuple = (stageNumber, -errors.size)

  override def compare(that: TableReadingErrors): Int = {
    tupleOrdering.compare(this.orderingTuple, that.orderingTuple)
  }

  override def toString = {
    if (errors.size == 0) {
      s"No errors (stage $stageNumber)"
    } else if (errors.size == 1) {
      s"${errors(0)} Stage number is $stageNumber."
    } else {
      s"${errors.size} errors in stage number $stageNumber. The first error is: ${errors(0)}"
    }
  }
}

object TableReadingErrors {

  /**
   * Special initial value which is worse than any result from any real stage.
   * This value can be used as an initial value in format detection heuristics.
   */
  val initial = TableReadingErrors(0, IndexedSeq(TableReadingError("No reading has been attempted yet.")))

  private type OrderingTuple = (Int, Int)

  private val tupleOrdering = implicitly(Ordering[OrderingTuple])
}
