package fi.pelam.csv.table

sealed trait Pipeline[RT, CT] {
  def map(f: TableReadingState[RT, CT] => TableReadingState[RT, CT]): Pipeline[RT, CT]
  def flatMap(inner: TableReadingState[RT, CT] => Pipeline[RT, CT]): Pipeline[RT, CT]
  def run(inputState: TableReadingState[RT, CT]): TableReadingState[RT, CT]
}

object Pipeline {

  case class Stage[RT, CT](phaseFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) extends Pipeline[RT, CT] {

    def map(mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = Stage[RT, CT](state => mapFunc(phaseFunc(state)))

    def flatMap(inner: TableReadingState[RT, CT] => Pipeline[RT, CT]): Pipeline[RT, CT] = FlatmapStage(this, inner)

    def run(inputState: TableReadingState[RT, CT]) = phaseFunc(inputState)
  }

  case class FlatmapStage[RT, CT](outer: Pipeline[RT, CT],
    inner: TableReadingState[RT, CT] => Pipeline[RT, CT],
    mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT] = (state: TableReadingState[RT, CT]) => state) extends Pipeline[RT, CT] {

    def map(newMapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = FlatmapStage(outer, inner,
      (state: TableReadingState[RT, CT]) => mapFunc(newMapFunc(state)))

    def flatMap(newInner: TableReadingState[RT, CT] => Pipeline[RT, CT]): Pipeline[RT, CT] = FlatmapStage(this, newInner)

    def run(inputState: TableReadingState[RT, CT]) = {
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
