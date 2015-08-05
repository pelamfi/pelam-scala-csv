package fi.pelam.csv.table

// TODO: Unit tests and a nice toString
case class TableReadingErrors(phase: Int = 0, errors: IndexedSeq[TableReadingError] = IndexedSeq()) {

  def add(moreErrors: TraversableOnce[TableReadingError]): TableReadingErrors = copy(errors = errors ++ moreErrors.toIndexedSeq)

  def add(error: TableReadingError): TableReadingErrors = copy(errors = errors :+ error)

  def noErrors = errors.isEmpty
}
