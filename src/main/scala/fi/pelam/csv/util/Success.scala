package fi.pelam.csv.util

/**
 * A trait that captures whether some state in some computation
 * is considered successful so far.
 */
// TODO: Better name?
trait Success {
  val success: Boolean
}
