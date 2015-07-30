package fi.pelam.csv.table

/**
 * A trait that captures whether some state in some computation
 * is considered successful so far.
 */
trait Success {
  val isSuccess: Boolean
}
