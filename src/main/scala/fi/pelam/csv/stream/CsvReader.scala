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
 * not been tested yet. The actual parsing is delegated to [[CsvReaderInternal]].
 * This class just implements the Scala `Iterator` interface on top of [[CsvReaderInternal]].
 *
 * == Error handling ==
 * If an error is encountered in the input a [[CsvReaderError]] will be returned
 * as the `Left` of `Either`. After this the iterator is exhausted and `hasNext` will return
 * false. Note that the input stream will be left open in this situation.
 * If `next` is called when `hasNext` is `false`, a `NoSuchElementException` will
 * be thrown as usual. See [[CsvReader!.throwOnError the method throwOnError]] for a shortcut to enable
 * exception based error handling.
 *
 * == Code example ==
 * This example code will pick the second column from the `csvData` and print the `toString`
 * of each `Cell` from that column. Note the use of `throwOnError` to bypass the `Either`
 * normally returned by the reader.
 *
 * {{{
 *
 * import fi.pelam.csv.cell._
 * import fi.pelam.csv.stream._
 *
 * val csvData = "foo,1,a\nbar,2,b\nbaz,3,c\n";
 *
 * val pickedCol = ColKey(1)
 *
 * for (cell <- new CsvReader(csvData).throwOnError; if cell.colKey == pickedCol) {
 *   println(cell)
 * }
 *
 * // Running the code above will print:
 * Cell containing '1' at Row 1, Column B (1)
 * Cell containing '2' at Row 2, Column B (1)
 * Cell containing '3' at Row 3, Column B (1)
 *
 * }}}
 *
 * @note A note on closing the input stream. The
 *       [[http://docs.oracle.com/javase/8/docs/api/java/io/Reader.html java.io.Reader]] is closed only if the
 *       CSV data exhausted. In error and other situations it is the responsibility of the caller to close the stream.
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
final class CsvReader(input: Reader, val separator: Char) extends Iterator[CsvReader.CellOrError] {

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
   * This method converts this reader into exception based error handling, which
   * may be useful in smaller applications that don't expect errors in input.
   *
   * A `RuntimeException` will be thrown when error is encountered. After this
   * the iterator is considered exhausted after the error.
   *
   * After being wrapped using this method, the reader produces simple
   * [[fi.pelam.csv.cell.StringCell StringCells]] instead of Scala's Either.

   * @note Note that the stream will be left open in this situation.
   */
  def throwOnError: Iterator[StringCell] = this.map {
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
