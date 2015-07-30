package fi.pelam.csv.table

sealed trait Pipeline[S <: Success] {
  def map(f: S => S): Pipeline[S]
  def flatMap(inner: S => Pipeline[S]): Pipeline[S]
  def run(inputState: S): S
}

object Pipeline {

  case class Stage[S <: Success](phaseFunc: S => S) extends Pipeline[S] {
    def map(mapFunc: S => S) = Stage[S](state => mapFunc(phaseFunc(state)))

    def flatMap(inner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, inner)

    def run(inputState: S) = phaseFunc(inputState)
  }

  case class FlatmapStage[S <: Success](outer: Pipeline[S],
    inner: S => Pipeline[S],
    mapFunc: S => S = (state: S) => state) extends Pipeline[S] {

    def map(newMapFunc: S => S) = FlatmapStage(outer, inner,
      (state: S) => mapFunc(newMapFunc(state)))

    def flatMap(newInner: S => Pipeline[S]): Pipeline[S] = FlatmapStage(this, newInner)

    def run(inputState: S) = {
      val outerResult = outer.run(inputState)
      if (outerResult.isSuccess) {
        mapFunc(inner(outerResult).run(outerResult)) // to call the inner func, we need state and we get the wrapper,
      } else {
        // Don't run further phases if one had error
        mapFunc(outerResult)
      }
    }
  }
}
