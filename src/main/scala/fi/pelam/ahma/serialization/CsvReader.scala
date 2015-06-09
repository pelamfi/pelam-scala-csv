package fi.pelam.ahma.serialization

import fi.pelam.ahma.serialization.CsvConstants._

import scala.collection.mutable

final object CsvReader {

  sealed abstract class State

  /**
   * "Zero width" initial state. If input ends here, zero cells will be emitted
   */
  case object StreamStart extends State

  /**
   * Zero width initial state for each cell from where we go to CellContent
   *
   * Used to handle case where final line ends without termination.
   */
  case object CellStart extends State

  /**
   * Within cell collecting data to emit cell. From this state we go to Quoted, Line or EndCell
   */
  case object CellContent extends State

  /**
   * Within cell collecting data to emit cell, but with quotes opened.
   */
  case object QuotedCellContent extends State

  /**
   * Cell content ready. Emit cell.
   */
  case object CellEnd extends State

  /**
   * Line end encountered
   */
  case object LineEnd extends State

  /**
   * Final state that signals that input stream has been exhausted and no more
   * cells will be emitted.
   */
  case object StreamEnd extends State
}

final class CsvReader(input: String, val separator: Char = defaultSeparatorChar) {

  import fi.pelam.ahma.serialization.CsvReader._

  private[this] var cell: Option[StringCell] = None

  private[this] var pos: Int = 0

  private[this] var lineStart: Int = 0

  private[this] var line: Int = 0

  private[this] var col: Int = 0

  private[this] var state: State = StreamStart

  private[this] var cellContentBuffer: StringBuilder = null

  private[this] var cellContentBufferedPos = 0

  private[this] def skipCharsUpToPos() = {
    cellContentBufferedPos = pos
  }

  private[this] def bufferCellContentUpToPos() = {
    cellContentBuffer.append(input.substring(cellContentBufferedPos, pos))
    skipCharsUpToPos()
  }

  private[this] def emitCell() = {
    if (cell.isDefined) {
      sys.error("Internal error. Cell aready emitted.")
    }

    cell = Some(StringCell(CellKey(line, col), cellContentBuffer.toString()))

    col += 1
  }

  private[this] def handleEndCell() = {
    bufferCellContentUpToPos()
    emitCell()
  }

  private[this] def char = input.charAt(pos)

  private[this] def peekCharAvailable = pos + 1 < input.length

  private[this] def peekChar = if (peekCharAvailable) {
    input.charAt(pos + 1)
  } else {
    '\u0000'
  }

  private[this] def atEnd = pos >= input.size

  private[this] def handleCellContentChar() = char match {
    case c: Char if c == separator => {
      bufferCellContentUpToPos()
      pos = pos + 1
      skipCharsUpToPos()
      state = CellEnd
    }

    case '"' => {
      bufferCellContentUpToPos()
      state = QuotedCellContent
      pos = pos + 1
      skipCharsUpToPos()
    }

    case '\r' => {
      if (peekCharAvailable && peekChar == '\n') {
        bufferCellContentUpToPos()
        pos = pos + 2
        skipCharsUpToPos()
        state = LineEnd
      } else {
        sys.error(s"Broken linefeed on $line. Expected LF after CR, but got char ${char.toInt}")
      }
    }

    case '\n' => {
      bufferCellContentUpToPos()
      pos = pos + 1
      skipCharsUpToPos()
      state = LineEnd
    }

    case _ => {
      pos = pos + 1
    }
  }

  def handleQuotedChar() = char match {
    case '"' => {
      bufferCellContentUpToPos()

      if (peekCharAvailable && peekChar == '"') {
        cellContentBuffer.append('"')
        pos = pos + 2
        skipCharsUpToPos()
      } else {
        state = CellContent
        pos = pos + 1
        skipCharsUpToPos()
      }
    }
    case '\r' | '\n' | ';' => {
      sys.error(s"Unclosed quote on line $line")
    }
    case _ => {
      pos = pos + 1
    }
  }

  // TODO: Make CsvReader stream like
  def read(): Option[StringCell] = {

    cell = None

    do {

      state match {
        case StreamStart => if (atEnd) {
          state = StreamEnd
          // Nothing to do anymore
        } else {
          state = CellStart
        }
        case CellContent => if (atEnd) {
          bufferCellContentUpToPos()
          // Gloss over final line without line feed
          state = LineEnd
        } else {
          handleCellContentChar()
        }
        case QuotedCellContent => if (atEnd) {
          sys.error("Input stream ended while processing quoted characters.")
        } else {
          handleQuotedChar()
        }
        case CellStart => {
          cellContentBuffer = new StringBuilder()
          if (atEnd) {
            // Gloss over zero width cell on final line without line feed
            state = LineEnd
          } else {
            state = CellContent
          }
        }
        case CellEnd => {
          emitCell()
          state = CellStart
        }
        case LineEnd => {
          emitCell()

          line = line + 1
          col = 0
          lineStart = pos

          if (atEnd) {
            state = StreamEnd
          } else {
            state = CellStart
          }
        }
        case StreamEnd => {
        }
      }

      // Loop until we can emit cell or input stream exhausted
    } while (state != StreamEnd && cell.isEmpty)

    cell
  }

  def readAll() = {
    val buffer = mutable.Buffer[StringCell]()

    while ( {
      val cell = read()

      if (cell.isDefined) {
        buffer += cell.get
      }

      cell.isDefined
    }) {
    }

    buffer
  }
}
