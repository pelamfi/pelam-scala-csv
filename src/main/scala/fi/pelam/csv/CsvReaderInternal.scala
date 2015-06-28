package fi.pelam.csv

import java.io.StringReader

/**
 * State machine based CSV parser which has single method interface which returns
 * an option of eiher cell or error.
 *
 * See [[CsvReader]] for friendlier Scala collections API like interface (Iterator).
 *
 * @param input stream to read CSV from. Input is read on as needed basis and closed if stream end is encountered.
 * @param separator separator char to use.
 */
final class CsvReaderInternal(input: java.io.Reader, val separator: Char) {

  import CsvReader._

  def this(input: String, separator: Char = CsvConstants.defaultSeparatorChar) = {
    this(new StringReader(input), separator)
  }

  def this(reader: java.io.Reader) = {
    this(reader, CsvConstants.defaultSeparatorChar)
  }


  private[this] var line: Int = 0

  private[this] var col: Int = 0

  private[this] def cellKey = CellKey(line, col)

  private[this] var state: State = StreamStart

  private[this] var cellContentBuffer: StringBuilder = null

  // An FSM state does not always consume just read the character.
  // These variables will pass the character to the next FSM state.
  private[this] var char: Int = 0
  private[this] var charConsumed = true

  /**
   * [[java.io.Reader#read()]] returns -1 at stream end.
   */
  private[this] def inputExhausted = char < 0

  private[this] def emitCell(): Option[CellOrError] = {
    val value = Some(Right(StringCell(CellKey(line, col), cellContentBuffer.toString())))
    col += 1
    value
  }

  /**
   * Process one character. Possibly change state.
   *
   * Always consumes the char
   */
  private[this] def handleCellContentChar(char: Char): Option[CellOrError] = {
    char match {
      case c if c == separator => {
        state = CellEnd
      }
      case '"' => {
        state = QuotedCellContent
      }
      case '\r' => {
        state = CarriageReturn
      }
      case '\n' => {
        state = LineEnd
      }
      case _ => {
        cellContentBuffer.append(char.asInstanceOf[Char])
      }
    }

    None
  }

  /**
   * Process one character. Possibly change state.
   *
   * Always consumes the char
   */
  private[this] def handleQuotedChar(char: Char): Option[CellOrError] = char match {
    case '"' => {
      state = PossibleEndQuote
      None
    }
    case '\r' | '\n' | ';' => {
      state = ErrorState
      Some(Left(Error(s"Unclosed quote on line $line", cellKey)))
    }
    case _ => {
      cellContentBuffer.append(char.asInstanceOf[Char])
      None
    }
  }

  /**
   * @return None when stream ends.
   */
  private[csv] def read(): Option[CellOrError] = {

    while (true) {

      // Read next char if needed and possible
      if (charConsumed && !inputExhausted) {
        char = input.read()

        if (inputExhausted) {
          input.close()
        } else {
          charConsumed = false
        }
      }

      // Process current FSM state and possibly emit cell or error
      val maybeCellOrError: Option[CellOrError] = state match {
        case StreamStart => if (inputExhausted) {
          state = StreamEnd
          None
          // Nothing to do anymore
        } else {
          state = CellStart
          None
        }
        case CellContent => if (inputExhausted) {
          // Gloss over final line without line feed
          state = LineEnd
          None
        } else {
          charConsumed = true
          handleCellContentChar(char.toChar)
        }
        case QuotedCellContent => if (inputExhausted) {
          state = ErrorState
          Some(Left(Error("Input stream ended while processing quoted characters.", cellKey)))
        } else {
          charConsumed = true
          handleQuotedChar(char.toChar)
        }
        case CarriageReturn => if (inputExhausted) {
          Some(Left(Error("Broken linefeed. Expected LF after CR, but stream ended.", cellKey)))
        } else {
          if (char == '\n') {
            charConsumed = true
            state = LineEnd
            None
          } else {
            Some(Left(Error("Broken linefeed. Expected LF after CR, but got '$char'.", cellKey)))
          }
        }
        case PossibleEndQuote => if (inputExhausted) {
          state = CellContent
          None
        } else {
          if (char == '"') {
            // Input quoted quote char.
            cellContentBuffer.append('"')
            charConsumed = true
            state = QuotedCellContent
            // More quoted stuff coming.
            None
          } else {
            state = CellContent
            // End quoted stuff
            None
          }
        }
        case CellStart => {
          cellContentBuffer = new StringBuilder()
          if (inputExhausted) {
            // Gloss over zero width cell on final line without line feed
            state = LineEnd
            None
          } else {
            state = CellContent
            None
          }
        }
        case CellEnd => {
          state = CellStart
          emitCell()
        }
        case LineEnd => {
          val cell = emitCell()

          line = line + 1
          col = 0

          state = if (inputExhausted) {
            StreamEnd
          } else {
            CellStart
          }

          cell
        }
        case StreamEnd => {
          None
        }
        case ErrorState => {
          None
        }
      }

      if (maybeCellOrError.isDefined) {
        return maybeCellOrError
      } else if (state == StreamEnd) {
        return None
      } else if (state == ErrorState) {
        return Some(Left(Error("CsvReader has encountered error.", cellKey)))
      }

      // Loop until we can emit cell, input stream exhausted or error has been encountered
    }

    sys.error("Will never get here")
  }
}