package fi.pelam.csv.stream

import java.io.Reader

import fi.pelam.csv.cell.{CellKey, StringCell}

/**
 * State machine based CSV parser which has single method interface which returns
 * an option of eiher cell or error.
 *
 * See [[CsvReader]] for friendlier Scala like interface (specifically Iterator).
 *
 * @param input stream to read CSV from. Input is read on as needed basis and closed if stream end is encountered.
 * @param separator separator char to use.
 *
 * @constructor create a parser from [[http://docs.oracle.com/javase/8/docs/api/java/io/Reader.html java.io.Reader]]
 *             and a separator character.
 */
final class CsvReaderInternal(input: Reader, separator: Char) {

  import CsvReaderInternal._

  private[this] var line: Int = 0

  private[this] var col: Int = 0

  private[this] def cellKey = CellKey(line, col)

  private[this] var state: State = StreamStart

  private[this] var cellContentBuffer: StringBuilder = null

  /** An FSM state does not always consume just read the character.
    * This variables together with [[charConsumed]] will pass the character
    * to the next FSM state.
    */
  private[this] var char: Int = 0

  /**
   * This flag tracks whether caracter in [[char]] is consumed.
   *
   * If this flag is true, first thing in the processing of next
   * state will be reading a new character to [[char]].
   */
  private[this] var charConsumed = true

  /**
   * [[http://docs.oracle.com/javase/8/docs/api/java/io/Reader.html#read-- java.io.Reader.read]] returns -1 at stream end.
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
      Some(Left(CsvReaderError("Unclosed quote", cellKey)))
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
          Some(Left(CsvReaderError("Input stream ended while processing quoted characters.", cellKey)))
        } else {
          charConsumed = true
          handleQuotedChar(char.toChar)
        }
        case CarriageReturn => if (inputExhausted) {
          Some(Left(CsvReaderError("Broken linefeed. Expected LF after CR, but stream ended.", cellKey)))
        } else {
          if (char == '\n') {
            charConsumed = true
            state = LineEnd
            None
          } else {
            Some(Left(CsvReaderError("Broken linefeed. Expected LF after CR, but got '$char'.", cellKey)))
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
        // Error state behaves like StreamEnd
        return None
      }

      // Loop until we can emit cell, input stream exhausted or error has been encountered
    }

    sys.error("Will never get here")
  }
}

final object CsvReaderInternal {

  /**
   * Base class for the finite state machine states
   * used in the parser.
   */
  sealed abstract class State

  /**
   * A "Zero width" initial state of the state machine.
   *
   * If input ends while in this state, zero cells will be emitted.
   */
  case object StreamStart extends State

  /**
   * Zero width initial state for each cell from where we go to [[CellContent]]
   *
   * Used to handle case where final line ends without termination.
   */
  case object CellStart extends State

  /**
   * Parsing a position inside a cell and collecting data to emit
   * the corresponding [[fi.pelam.csv.cell.StringCell]] object.
   *
   * From this state the state machine transitions to
   * [[QuotedCellContent]], [[LineEnd]] or [[CellEnd]].
   */
  case object CellContent extends State

  /**
   * Parsing a position inside a cell and collecting data to emit
   * the corresponding [[fi.pelam.csv.cell.StringCell]] object.
   *
   * The difference to ordinary [[CellContent]] state is that
   * a quote has been encountered.
   *
   * A cell may contain multiple quoted sections, although usually
   * the whole cell content is quoted or none.
   */
  case object QuotedCellContent extends State

  /**
   * State machine transitions to this state from state
   * [[QuotedCellContent]] when the quote character is encountered.
   * If it is just a lone quote, the quoted section ends.
   *
   * However, two quote characters together are interpreted as an escaped
   * quote character. At least Excel and Google Docs seem to adhere
   * to this convention.
   *
   * This state exists to allow separating these two cases.
   */
  case object PossibleEndQuote extends State

  /**
   * Cell content is ready. Emit the [[fi.pelam.csv.cell.StringCell]] object.
   */
  case object CellEnd extends State

  /**
   * This is a state for handling CR LF style line termination.
   */
  case object CarriageReturn extends State

  /**
   * A state signaling that current line has ended.
   */
  case object LineEnd extends State

  /**
   * Final state that signals that input stream has been exhausted and no more
   * cells will be emitted.
   *
   * Subsequent calls to read will produce None.
   */
  case object StreamEnd extends State

  /**
   * Parser won't continue after encountering first error.
   *
   * Subsequent calls to read will produce None.
   *
   * Parser will then remain in this state.
   */
  case object ErrorState extends State

  /**
   * The type of of output of this class.
   *
   * Errors are separated by using Scala's Either type.
   */
  type CellOrError = Either[CsvReaderError, StringCell]
}
