package fi.pelam.csv.table

sealed trait Pipeline[S <: Success] {
  def map(f: S => S): Pipeline[S]
  def flatMap(inner: S => Pipeline[S]): Pipeline[S]
  def run(inputState: S): S
}

object Pipeline {
  case class Stage[S <: Success](stageFunction: S => S) extends Pipeline[S] {
    override def map(mapFunc: S => S) = Stage[S](state => mapFunc(stageFunction(state)))

    override def flatMap(inner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, inner)

    override def run(inputState: S) = stageFunction(inputState)
  }

  case class FlatmapStage[S <: Success](outer: Pipeline[S],
    inner: S => Pipeline[S],
    mapFunc: S => S = (state: S) => state) extends Pipeline[S] {

    override def map(newMapFunc: S => S) = FlatmapStage(outer, inner,
      (state: S) => mapFunc(newMapFunc(state)))

    override def flatMap(newInner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, newInner)

    override def run(inputState: S) = {
      val outerResult = outer.run(inputState)
      if (outerResult.isSuccess) {
        // Call the inner (and downstream in the pipeline) stages
        mapFunc(inner(outerResult).run(outerResult))
      } else {
        // Don't run further stages if one had error
        mapFunc(outerResult)
      }
    }
  }
}
