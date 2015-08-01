package fi.pelam.csv.util

import org.junit.Test
import org.junit.Assert._

class PipelineTest {

  case class State(override val success: Boolean = true, value: Int = 0) extends Success

  @Test
  def testRun: Unit = {

    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState

    assertEquals(State(true, 111), pipeline.run(State()))
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

    assertEquals(State(true, 1111), pipeline2.run(State()))
  }

  @Test
  def testRunWithMap: Unit = {
    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState.copy(value = finalState.value * 2)

    assertEquals(State(true, 222), pipeline.run(State()))
  }

  @Test
  def testRunWithFail: Unit = {

    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(success = false, value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState

    assertEquals(State(false, 11), pipeline.run(State()))
  }

  @Test
  def testRunWithFailAndMap: Unit = {
    val pipeline = for (
      _ <- Pipeline.Stage((state: State) => state.copy(value = state.value + 1));
      _ <- Pipeline.Stage((state: State) => state.copy(success = false, value = state.value + 10));
      finalState <- Pipeline.Stage((state: State) => state.copy(value = state.value + 100))
    ) yield finalState.copy(value = finalState.value * 2)

    assertEquals(State(false, 11), pipeline.run(State()))
  }

}