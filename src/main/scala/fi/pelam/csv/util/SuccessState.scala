package fi.pelam.csv.util

/**
 * A trait that captures whether some state in some computation
 * is considered successful so far. This is used by [[Pipeline]].
 */
trait SuccessState {
  val success: Boolean
}
