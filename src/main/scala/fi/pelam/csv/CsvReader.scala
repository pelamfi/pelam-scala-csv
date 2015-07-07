package fi.pelam.csv

import java.io.{Reader, StringReader}

/**
 * CSV parser that implements Scala iterator interface.
 *
 * Does parsing in streaming fashion ie. you could even handle files
 * larger that what would fit in memory.
 *
 * Actual parsing is delegated to [[CsvReaderInternal]]. This class
 * just implements the Scala iterator interface on top of it.
 *
 * @see TableReader for a friendlier non streaming API.
 *
 * @param input  Input can be string or [[java.io.Reader]]. Be mindful of the character set.
 * @param separator Optional separator character
 */
// TODO: Code example for CsvReader
final class CsvReader(input: Reader, val separator: Char) extends Iterator[CsvReader.CellOrError] {

  import CsvReader._

  /**
   * Alternate constructor for CsvReader providing string input.
   * This exists mainly to make tests and code examples shorter.
   */
  def this(inputString: String, separator: Char = CsvConstants.defaultSeparatorChar) = this(new StringReader(inputString), separator)

  /**
   * Alternate constructor using default separator which is comma.
   */
  def this(input: Reader) = this(input, CsvConstants.defaultSeparatorChar)

  private[this] val internal = new CsvReaderInternal(input, separator)

  private[this] var cell: Option[CellOrError] = None

  // Start internal reader so that hasNext works
  cell = internal.read()

  override def next(): CellOrError = nextOption.get

  override def hasNext: Boolean = cell.isDefined

  def nextOption(): Option[CellOrError] = {
    val prereadCell = cell
    // Read next cell, so hasNext can work
    cell = internal.read()
    prereadCell
  }

  /**
   * Convert this CscReader into a form which throws upon encountering an
   * error instead of returning [[fi.pelam.csv.CsvReader#Error]]
   * @return
   */
  def raiseOnError: Iterator[StringCell] = this.map {
    case Left(e: Error) => sys.error(e.toString)
    case Right(stringCell) => stringCell
  }
}

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

  /**
   * Parser won't continue after encountering first error.
   * Parser will then remain in this state.
   */
  case object ErrorState extends State

  case class Error(message: String, at: CellKey) {
    override def toString = s"Error parsing CSV at $at: $message"
  }

  type CellOrError = Either[Error, StringCell]
}
