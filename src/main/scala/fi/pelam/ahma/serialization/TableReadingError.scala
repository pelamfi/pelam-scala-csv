package fi.pelam.ahma.serialization

case class TableReadingError(msg: String) extends RuntimeException(msg) {

}
