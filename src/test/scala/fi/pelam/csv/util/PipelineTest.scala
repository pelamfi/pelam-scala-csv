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

import org.junit.Assert._
import org.junit.Test

class PipelineTest {

  case class State(override val success: Boolean = true,
    value: Int = 0,
    override val stageNumber: Int = 0)
    extends LastStageResult[State] {

    override def stageNumberIncremented(): State = copy(stageNumber = stageNumber + 1)
  }

  @Test
  def testRun: Unit = {

    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState

    assertEquals(State(true, 111, 3), pipeline.run(State()))
  }

  @Test
  def testRunContinued: Unit = {

    val pipeline1 = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 10))
    ) yield finalState

    val pipeline2 = for (
      _ <- pipeline1;
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1000))
    ) yield finalState

    assertEquals(State(true, 1111, 4), pipeline2.run(State()))
  }

  @Test
  def testRunWithMap: Unit = {
    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState.copy(value = finalState.value * 2)

    assertEquals(State(true, 222, 3), pipeline.run(State()))
  }

  @Test
  def testRunWithFail: Unit = {

    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(success = false, value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState

    assertEquals(State(false, 11, 2), pipeline.run(State()))
  }

  @Test
  def testRunWithFailAndMap: Unit = {
    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(success = false, value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState.copy(value = finalState.value * 2)

    assertEquals("Final map is not run", State(false, 11, 2), pipeline.run(State()))
  }

}