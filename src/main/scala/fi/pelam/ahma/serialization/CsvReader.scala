package fi.pelam.ahma.serialization

import java.io.StringReader

import fi.pelam.ahma.serialization.CsvConstants._

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
   * Double quotes is quote character, single quote ends quotes.
   * This state checks which it is.
   */
  case object PossibleEndQuote extends State

  /**
   * Cell content ready. Emit cell.
   */
  case object CellEnd extends State

  /**
   * For handling CR LF style line termination
   */
  case object CarriageReturn extends State

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

final class CsvReader(input: java.io.Reader, val separator: Char) extends Iterator[StringCell] {

  def this(input: String, separator: Char = defaultSeparatorChar) = {
    this(new StringReader(input), separator)
  }

  def this(reader: java.io.Reader) = {
    this(reader, defaultSeparatorChar)
  }

  import fi.pelam.ahma.serialization.CsvReader._

  private[this] var cell: Option[StringCell] = None

  private[this] var line: Int = 0

  private[this] var col: Int = 0

  private[this] var state: State = StreamStart

  private[this] var cellContentBuffer: StringBuilder = null

  private[this] var char: Int = 0

  private[this] var charConsumed = true

  // Start iterator so that hasNext works
  readNext()

  override def next(): StringCell = nextOption.get

  override def hasNext: Boolean = cell.isDefined

  /**
   * [[java.io.Reader#read()]] returns -1 at stream end.
   */
  private[this] def atEnd = char < 0

  private[this] def emitCell() = {
    if (cell.isDefined) {
      sys.error("Internal error. Cell aready emitted.")
    }

    cell = Some(StringCell(CellKey(line, col), cellContentBuffer.toString()))

    col += 1
  }

  private[this] def handleEndCell() = {
    emitCell()
  }

  private[this] def handleCellContentChar() = char match {
    case c if c == separator => {
      charConsumed = true
      state = CellEnd
    }

    case '"' => {
      state = QuotedCellContent
      charConsumed = true
    }

    case '\r' => {
      charConsumed = true
      state = CarriageReturn
    }

    case '\n' => {
      charConsumed = true
      state = LineEnd
    }

    case _ => {
      cellContentBuffer.append(char.asInstanceOf[Char])
      charConsumed = true
    }
  }

  private[this] def handleQuotedChar() = char match {
    case '"' => {
      state = PossibleEndQuote
      charConsumed = true
    }
    case '\r' | '\n' | ';' => {
      sys.error(s"Unclosed quote on line $line")
    }
    case _ => {
      cellContentBuffer.append(char.asInstanceOf[Char])
      charConsumed = true
    }
  }

  def nextOption(): Option[StringCell] = {
    val prereadCell = cell
    readNext()
    prereadCell
  }

  private[this] def readNext(): Unit = {

    cell = None

    do {

      if (charConsumed && !atEnd) {
        char = input.read()

        if (atEnd) {
          input.close()
        }

        charConsumed = false
      }

      state match {
        case StreamStart => if (atEnd) {
          state = StreamEnd
          // Nothing to do anymore
        } else {
          state = CellStart
        }
        case CellContent => if (atEnd) {
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
        case CarriageReturn => if (atEnd) {
          sys.error(s"Broken linefeed on $line. Expected LF after CR, but stream ended.")
        } else {
          if (char == '\n') {
            charConsumed = true
            state = LineEnd
          } else {
            sys.error(s"Broken linefeed on $line. Expected LF after CR, but got '$char'.")
          }
        }
        case PossibleEndQuote => if (atEnd) {
          state = CellContent
        } else {
          if (char == '"') {
            // Input quoted quote char.
            cellContentBuffer.append('"')
            charConsumed = true
            state = QuotedCellContent
            // More quoted stuff coming.
          } else {
            state = CellContent
            // End quoted stuff
          }
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
  }
}
