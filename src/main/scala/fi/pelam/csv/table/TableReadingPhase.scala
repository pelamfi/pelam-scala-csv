package fi.pelam.csv.table

object TableReadingPhase {

  case class Phase[RT, CT](phaseFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) extends TableReadingPhase[RT, CT] {

    def map(mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = Phase[RT, CT](state => mapFunc(phaseFunc(state)))

    def flatMap(inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT] = PhaseFlatmap(this, inner)

    def run(inputState: TableReadingState[RT, CT]) = phaseFunc(inputState)
  }

  case class PhaseFlatmap[RT, CT](outer: TableReadingPhase[RT, CT],
    inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT],
    mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT] = (state: TableReadingState[RT, CT]) => state) extends TableReadingPhase[RT, CT] {

    def map(newMapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = PhaseFlatmap(outer, inner,
      (state: TableReadingState[RT, CT]) => mapFunc(newMapFunc(state)))

    def flatMap(newInner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT] = PhaseFlatmap(this, newInner)

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

sealed trait TableReadingPhase[RT, CT] {
  def map(f: TableReadingState[RT, CT] => TableReadingState[RT, CT]): TableReadingPhase[RT, CT]
  def flatMap(inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT]
  def run(inputState: TableReadingState[RT, CT]): TableReadingState[RT, CT]
}