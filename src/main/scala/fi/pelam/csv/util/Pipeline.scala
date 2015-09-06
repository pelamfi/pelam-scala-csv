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
 * A monad abstraction that can be used to build up a chain of
 * transformations of state. An input state can then be passed
 * through the pipeline with the [[Pipeline!.run run]] method.
 *
 * The state is queried for success via the trait [[LastStageResult]].
 *
 * If the state does not report as success after executing a stage,
 * the rest of the pipeline is bypassed.
 *
 * == Code example ==
 *
 * {{{
 *   case class State(override val success: Boolean = true, value: Int = 0) extends SuccessState
 *
 *   val pipeline = for (
 *     _ <- Pipeline.Stage((a: State) => a.copy(value = a.value + 1));
 *     _ <- Pipeline.Stage((a: State) => a.copy(value = a.value + 10));
 *     finalState <- Pipeline.Stage((a: State) => a.copy(value = a.value + 100))
 *   ) yield finalState
 *
 *   println(pipeline.run(State()))
 *   // Will print State(true,111)
 * }}}
 *
 * @tparam S The type of the state to be threaded through pipeline stages.
 */
// TODO: Make it more clear in the example what the stages do.
// TODO: Is it accurate to call this a monad?
sealed trait Pipeline[S <: LastStageResult[S]] {
  def map(f: S => S): Pipeline[S]

  def flatMap(inner: S => Pipeline[S]): Pipeline[S]

  def run(inputState: S): S
}

object Pipeline {

  case class Stage[S <: LastStageResult[S]](stageFunc: S => S) extends Pipeline[S] {
    override def map(mapFunc: S => S) = Stage[S](state => mapFunc(stageFunc(state)))

    override def flatMap(inner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, inner)

    override def run(inputState: S) = stageFunc(inputState).stageNumberIncremented()
  }

  private case class FlatmapStage[S <: LastStageResult[S]](outer: Pipeline[S],
    inner: S => Pipeline[S],
    mapFunc: S => S = (state: S) => state) extends Pipeline[S] {

    override def map(newMapFunc: S => S) = FlatmapStage(outer, inner,
      (state: S) => mapFunc(newMapFunc(state)))

    override def flatMap(newInner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, newInner)

    override def run(inputState: S) = {
      val outerResult = outer.run(inputState)

      if (outerResult.success) {
        // Call the inner (and downstream in the pipeline) stages.
        mapFunc(inner(outerResult).run(outerResult))
      } else {
        // Don't run further stages if one had error, but run the final map.
        mapFunc(outerResult)
      }
    }
  }

}
