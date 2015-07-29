package fi.pelam.csv.stream

import java.io.{Reader, StringReader}

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell.StringCell

/**
 * This class is part of the lower level API for processing CSV data.
 * This is a CSV parser that produces the data through the
 * [[http://www.scala-lang.org/api/current/index.html#scala.collection.Iterator scala.collection.Iterator]] trait.
 * The data is read into a sequence of [[fi.pelam.csv.cell.StringCell]] instances.
 * [[fi.pelam.csv.cell.StringCell StringCells]] can be written back to disk with [[CsvWriter]].
 *
 * This class does parsing in streaming fashion ie. you should be able to
 * handle files larger than what can fit in RAM, though this has
 * not been tested yet.
 *
 * Actual parsing is delegated to [[CsvReaderInternal]]. This class
 * just implements the Scala iterator interface on top of [[CsvReaderInternal]].
 *
 * @see [[fi.pelam.csv.table.TableReader TableReader for a friendlier non streaming API.]]
 *
 * @param input  Input can be string or [[http://docs.oracle.com/javase/8/docs/api/java/io/Reader.html java.io.Reader]].
 *               Be mindful of the character set.
 *
 * @param separator Optional non-default separator character
 *
 * @constructor Create a new reader while specifying the separator character.
 */
// TODO: Code example for CsvReader
final class CsvReader (input: Reader, val separator: Char) extends Iterator[CsvReader.CellOrError] {

  import CsvReader._

  /**
   * Alternate constructor for CsvReader providing string input and optionally specifying
   * separator character.
   *
   * This alternate constructor exists mainly to make tests and code examples shorter.
   */
  def this(inputString: String, separator: Char = CsvConstants.defaultSeparatorChar) =
    this(new StringReader(inputString), separator)

  /**
   * Alternate constructor for just using default separator which is comma.
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
   * Convert this instance into a form which throws upon encountering an
   * error instead of returning [[CsvReaderError]]
   * @return
   */
  def raiseOnError: Iterator[StringCell] = this.map {
    case Left(e: CsvReaderError) => sys.error(e.toString)
    case Right(stringCell) => stringCell
  }
}

final object CsvReader {

  /**
   * The type of output of this class. Matches the type [[CsvReaderInternal.CellOrError]].
   */
  type CellOrError = CsvReaderInternal.CellOrError

}
