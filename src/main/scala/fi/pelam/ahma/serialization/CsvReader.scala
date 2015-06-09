package fi.pelam.ahma.serialization

import fi.pelam.ahma.serialization.CsvConstants._

import scala.collection.mutable

final object CsvReader {

  sealed abstract class State

  case object CellContent extends State

  case object Quoted extends State
}

final class CsvReader(input: String, val separator: Char = defaultSeparatorChar) {

  import fi.pelam.ahma.serialization.CsvReader._

  private[this] var cell: Option[StringCell] = None

  private[this] var pos: Int = 0

  private[this] var lineStart: Int = 0

  private[this] var line: Int = 0

  private[this] var col: Int = 0

  private[this] var state: State = CellContent

  private[this] var cellContentBuffer: StringBuilder = new StringBuilder()

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

  private[this] def handleStartLine(): Unit = {
    skipCharsUpToPos()
    col = 0
    lineStart = pos
  }

  private[this] def char = input.charAt(pos)

  private[this] def peekCharAvailable = pos + 1 < input.length

  private[this] def peekChar = if (peekCharAvailable) {
    input.charAt(pos + 1)
  } else {
    '\u0000'
  }

  private[this] def atEnd = pos >= input.size

  private[this] def unprocessedData = cellContentBufferedPos < input.size

  private[this] def handleCellContent() = char match {
    case c: Char if c == separator => {
      handleEndCell()
      pos = pos + 1
      skipCharsUpToPos()
    }

    case '"' => {
      bufferCellContentUpToPos()
      state = Quoted
      pos = pos + 1
      skipCharsUpToPos()
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

  def handleQuoted() = char match {
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

    while (!atEnd && cell == None) {

      state match {
        case CellContent => handleCellContent()
        case Quoted => handleQuoted()
      }
    }

    if (unprocessedData && atEnd && cell.isEmpty) {
      // Gloss over missing final linefeed
      handleEndLine()
    }

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
