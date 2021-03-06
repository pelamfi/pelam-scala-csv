/*
 * This file is part of pelam-scala-csv
 *
 * Copyright © Peter Lamberg 2015 (pelam-scala-csv@pelam.fi)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fi.pelam.csv.stream

import java.io.{Reader, StringReader}

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell.StringCell

import scala.collection.AbstractIterator

/**
 * This class is part of the lower level API for processing CSV data.
 * This is a CSV parser that produces the data through the
 * [[http://www.scala-lang.org/api/current/index.html#scala.collection.Iterator scala.collection.Iterator]] trait.
 * The data is read into a sequence of [[fi.pelam.csv.cell.StringCell StringCell]] instances.
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

  import fi.pelam.csv.cell._
  import fi.pelam.csv.stream._

  val csvData =
      "apple,0.99,3\n" +
      "orange,1.25,2\n" +
      "banana,0.80,4\n"

  val pickedCol = ColKey(1)

  for (cell <- new CsvReader(csvData).throwOnError; if cell.colKey == pickedCol) {
    println(cell)
  }

  // Running the code above will print the cells from the second column:
  Cell containing '0.99' at Row 1, Column B (1)
  Cell containing '1.25' at Row 2, Column B (1)
  Cell containing '0.80' at Row 3, Column B (1)

  }}}
 *
 * @note A note on closing the input stream. The
 *       [[http://docs.oracle.com/javase/8/docs/api/java/io/Reader.html java.io.Reader]] is closed only if the
 *       CSV data exhausted. In the case of an error and in the case of stopping to call the `read`
 *       method before the input ends it is the responsibility of the caller to close the stream.
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
final class CsvReader(input: Reader, val separator: Char) extends AbstractIterator[CsvReader.CellOrError] {

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

  // Start internal reader so that hasNext works
  private[this] var cell: Option[CellOrError] = internal.read()

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

object CsvReader {

  /**
   * The type of output of this class. Matches the type [[CsvReaderInternal.CellOrError]].
   */
  type CellOrError = CsvReaderInternal.CellOrError

}
