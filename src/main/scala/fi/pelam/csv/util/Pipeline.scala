package fi.pelam.csv.util

/**
 * A monad abstraction that can be used to build up a chain of
 * transformations of state. An input state can then be passed
 * through the pipeline with the [[Pipeline!.run run]] method.
 *
 * The state is queried for success via the trait [[SuccessState]].
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
// TODO: Is it accurate to call this a monad?
sealed trait Pipeline[S <: SuccessState] {
  def map(f: S => S): Pipeline[S]

  def flatMap(inner: S => Pipeline[S]): Pipeline[S]

  def run(inputState: S): S
}

object Pipeline {

  case class Stage[S <: SuccessState](stageFunction: S => S) extends Pipeline[S] {
    override def map(mapFunc: S => S) = Stage[S](state => mapFunc(stageFunction(state)))

    override def flatMap(inner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, inner)

    override def run(inputState: S) = stageFunction(inputState)
  }

  private case class FlatmapStage[S <: SuccessState](outer: Pipeline[S],
    inner: S => Pipeline[S],
    mapFunc: S => S = (state: S) => state) extends Pipeline[S] {

    override def map(newMapFunc: S => S) = FlatmapStage(outer, inner,
      (state: S) => mapFunc(newMapFunc(state)))

    override def flatMap(newInner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, newInner)

    override def run(inputState: S) = {
      val outerResult = outer.run(inputState)
      if (outerResult.success) {
        // Call the inner (and downstream in the pipeline) stages
        mapFunc(inner(outerResult).run(outerResult))
      } else {
        // Don't run further stages if one had error
        mapFunc(outerResult)
      }
    }
  }

}
