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

package fi.pelam.csv.util

/**
 * A trait that captures whether some state in the previous stage of some computation
 * is considered successful. This is used by [[Pipeline]].
 */
trait LastStageResult[T <: LastStageResult[T]] {

  val success: Boolean

  /**
   * Simple counter for stages executed so far. Should start initially at zero
   * and increment by one after each stage.
   *
   * This is used in error handling schemes to compare how far in the pipeline
   * the computation got before errors.
   *
   * Specifically this feature is used by [[fi.pelam.csv.table.TableReaderEvaluator]]
   * in the ordering of [[fi.pelam.csv.table.TableReadingErrors]].
   */
  val stageNumber: Int

  /**
   * [[Pipeline]] uses this to update stage number automatically.
   *
   * @return Otherwise identical object, but with stage number incremented.
   */
  def stageNumberIncremented(): T
}
