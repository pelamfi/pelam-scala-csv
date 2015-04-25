package fi.pelam.ahma.serialization

import scala.collection.mutable

final object CsvParser {

  sealed abstract class State

  case object CellContent extends State

  case object Quoted extends State

}

final class CsvParser(input: String, val separator: Char = ',') {

  import fi.pelam.ahma.serialization.CsvParser._

  val builder = mutable.Buffer[SimpleCell]()

  var pos: Int = 0

  var lineStart: Int = 0

  var line: Int = 0

  var col: Int = 0

  var state: State = CellContent

  var cellContentBuffer: StringBuilder = new StringBuilder()

  var cellContentBufferedPos = 0

  private[this] def skipCellContentUpToPos() = {
    cellContentBufferedPos = pos
  }

  private[this] def bufferCellContentUpToPos() = {
    cellContentBuffer.append(input.substring(cellContentBufferedPos, pos))
    skipCellContentUpToPos()
  }

  private[this] def emitCell() = {
    builder += SimpleCell(CellKey(line, col), cellContentBuffer.toString())
    col += 1
    cellContentBuffer = new StringBuilder()
  }

  private[this] def handleEndCell() = {
    bufferCellContentUpToPos()
    emitCell()
  }

  private[this] def handleEndLine() = {
    handleEndCell()
    line = line + 1
  }

  def handleStartLine(): Unit = {
    skipCellContentUpToPos()
    col = 0
    lineStart = pos
  }

  def parse(): mutable.Buffer[SimpleCell] = {

    while (pos < input.length) {
      val char = input.charAt(pos)

      val peekCharAvailable = pos + 1 < input.length

      val peekChar = if (peekCharAvailable) {
        input.charAt(pos + 1)
      } else {
        '\u0000'
      }

      state match {

        case CellContent => char match {
          case c: Char if c == separator => {
            handleEndCell()
            pos = pos + 1
            skipCellContentUpToPos()
          }

          case '"' => {
            bufferCellContentUpToPos()
            state = Quoted
            pos = pos + 1
            skipCellContentUpToPos()
          }

          case '\r' => {
            if (peekCharAvailable && peekChar == '\n') {
              handleEndLine()
              pos = pos + 2
              handleStartLine()
            } else {
              sys.error(s"Broken linefeed on $line. Expected LF after CR, but got char ${char.toInt}")
            }
          }

          case '\n' => {
            handleEndLine()
            pos = pos + 1
            handleStartLine()
          }

          case _ => {
            pos = pos + 1
          }
        }

        case Quoted => char match {
          case '"' => {
            bufferCellContentUpToPos()

            if (peekCharAvailable && peekChar == '"') {
              cellContentBuffer.append('"')
              pos = pos + 2
              skipCellContentUpToPos()
            } else {
              state = CellContent
              pos = pos + 1
              skipCellContentUpToPos()
            }
          }
          case '\r' | '\n' | ';' => {
            sys.error(s"Unclosed quote on line $line")
          }
          case _ => {
            pos = pos + 1
          }
        }
      }
      ()
    }

    if (pos != lineStart) {
      sys.error(s"Last line $line was not terminated with line feed.")
    }

    builder
  }

}
