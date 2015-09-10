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

package fi.pelam.csv.table

import java.io.BufferedReader

import fi.pelam.csv.cell._
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.{Pipeline, SortedBiMap}

// @formatter:off IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
/**
 * This class is part of the the higher level API for reading,
 * writing and processing CSV data.
 *
 * [[fi.pelam.csv.stream.CsvReader The simpler stream based API]] is enough for
 * many scenarios, but if several different sets of data will be pulled from
 * the same CSV file and the structure of the CSV file is not rigid, this API may
 * be a better fit.
 *
 * The result of calling `read` method on this class will be an instance of [[Table]] class.
 * The table is an immutable data structure for holding and processing data
 * in a spreadsheet program like format.
 *
 * [[TableWriter]] is the counterpart of this class for writing [[Table]] out to disk for example.
 *
 * == Code Example ==
 *
 * This example parses a small bit of CSV data in which column types are defined
 * on the first row.
 *
 * {{{
    import fi.pelam.csv.table._
    import fi.pelam.csv.cell._
    import TableReaderConfig._

    // Create a TableReader that parses a small bit of CSV data in which the
    // column types are defined on the first row.
    val reader = new TableReader[String, String, SimpleMetadata](

      // An implicit from the object TableReaderConfig converts the string
      // to a function providing streams.
      openStream =
        "product,price,number\n" +
        "apple,0.99,3\n" +
        "orange,1.25,2\n" +
        "banana,0.80,4\n",

      // The first row is the header, the rest are data.
      rowTyper = makeRowTyper({
        case (CellKey(0, _), _) => "header"
        case _ => "data"
      }),

      // First row defines column types.
      colTyper = makeColTyper({
        case (CellKey(0, _), colType) => colType
      }),

      // Convert cells on the "data" rows in the "number" column to integer cells.
      cellUpgrader = makeCellUpgrader({
        case CellType("data", "number") => IntegerCell.defaultParser
        case CellType("data", "price") => DoubleCell.defaultParser
      }))

    // Get values from cells in column with type "product" on rows with type "data."
    table.getSingleCol("data", "product").map(_.value).toList
    // Will give List("apple", "orange", "banana")

    // Get values from cells in column with type "number" on rows with type "data."
    table.getSingleCol("number", "data").map(_.value).toList)
    // Will give List(0.99, 1.25, 0.8)
 * }}}
 *
 * == CSV format detection heuristics ==
 * One simple detection heuristic is implemented in [[DetectingTableReader]]
 *
 * Since deducing whether correct parameters like character set were used in reading a CSV
 * file without any extra knowledge is impossible, this class supports implementing a custom
 * format detection algorithm by client code.
 *
 * The table reading is split to stages to allow implementing format detection heuristics that
 * lock some variables during the earlier stages and then proceeding to later stages. Unfortunately
 * there is currently no example or implementation of this idea.
 *
 * Locking some variables and then proceeding results in more efficient algorithm
 * than exhaustive search of the full set of combinations (character set, locale, separator etc).
 *
 * The actual detection heuristic is handled outside this class. The idea is that the
 * detection heuristic class uses this repeatedly with varying parameters until some criterion
 * is met. The criterion for ending detection could be that zero errors is detected.
 * If no combination of parameters gives zero errors, then the heuristic could just pick
 * the solution which gave errors in the latest stage and then the fewest errors.
 *
 * == Stages ==
 * @note This is about the internal structure of `TableReader` processing.
 *
 * The table reading is split into four stages.
 *
 * The table reading process may fail and terminate at each phase. Then an incomplete Table object
 * will be returned together with the errors detected so far.
 *
 * The table reading is split to stages to allow implementing format detection heuristics
 * in a structured manner.
 *
 *  - `csvReadingStage` Parse CSV byte data to cells. Depends on `charset` and `separator` provided
 * via the `metadata` parameter.
 *
 *  - `rowTypeDetectionStage` Detect row types (hard coded or based on cell contents). The `rowTyper` parameter
 * is used in this stage.
 *
 *  - `colTypeDetectionStage` Detect column types (hard coded or based on row types and cell contents). The `colTyper` parameter
 * is used in this stage.
 *
 *  - `cellUpgradeStage` Upgrade cells based on cell types, which are combined from row and column types. The `cellUpgrader`
 * parameter is used in this stage.
 *
 * @param openInputStream A function that returns input stream from which the data should be read.
 *                        The function is called once for every `read` call.
 *
 * @param metadata A custom metadata object which will be passed to the resulting `Table` object.
 *
 * @param rowTyper Partial function used in `rowTypeDetectionStage`
 *
 * @param colTyper Partial function used in `colTypeDetectionStage`
 *
 * @param cellUpgrader Partial function used in `cellUpgradeStage`
 *
 * @tparam RT The client specific row type.
 *
 * @tparam CT The client specific column type.
 *
 * @tparam M The type of the `metadata` parameter. Must be a sub type of [[TableMetadata]].
 *           This specifies the character set and separator to use when reading the CSV data from the input stream.
 */
// @formatter:on IntelliJ 14.1 (Scala plugin) formatter messes up Scaladoc
class TableReader[RT, CT, M <: TableMetadata](
  val openStream: TableReader.StreamOpener,
  val tableMetadata: M = SimpleMetadata(),
  val rowTyper: TableReader.RowTyper[RT] = PartialFunction.empty,
  val colTyper: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
  val cellUpgrader: TableReader.CellUpgrader[RT, CT] = PartialFunction.empty
  ) {

  /**
   * The [[Table]] type returned by Read
   */
  type ResultTable = Table[RT, CT, M]

  /**
   * Internal type for state passed through the [[.pipeline]].
   */
  private type State = TableReadingState[RT, CT]

  /**
   * The main method in this class. Can be called several times.
   * The input stream is opened and closed once per each call.
   *
   * If there are no errors [[TableReadingErrors.noErrors]] is `true`.
   *
   * @return a pair with a [[.TableReader]] and [[TableReadingErrors]].
   */
  def read(): (ResultTable, TableReadingErrors) = {

    val initial = TableReadingState[RT, CT]()

    val result = pipeline.run(initial)

    (Table(result.cells, result.rowTypes, result.colTypes, tableMetadata), result.errors)
  }

  /**
   * This method extends the basic `read` method with exception based error handling,
   * which may be useful in smaller applications that don't expect or handle
   * errors in input.
   *
   * A `RuntimeException` will be thrown when error is encountered.
   */
  def readOrThrow(): ResultTable = {
    val (table, errors) = read()
    if (errors.noErrors) {
      table
    } else {
      sys.error(errors.toString)
    }
  }

  override def toString() = s"TablerReader(tableMetadata = $tableMetadata)"

  // Chain stages together into one pipeline and setup error handling
  // using the Pipeline abstraction.
  private[csv] def pipeline = {
    for (_ <- Pipeline.Stage(csvReadingStage);
         _ <- Pipeline.Stage(rowTypeDetectionStage);
         _ <- Pipeline.Stage(colTypeDetectionStage);
         x <- Pipeline.Stage(cellUpgradeStage)) yield x
  }

  private[csv] def csvReadingStage(input: State): State = {
    val inputStream = openStream()

    try {

      val inputReader: java.io.Reader = new BufferedReader(new java.io.InputStreamReader(inputStream, tableMetadata.charset), 1024)

      val csvReader = new CsvReader(inputReader, separator = tableMetadata.separator)

      val (lefts, rights) = csvReader.partition(_.isLeft)

      val errors = lefts.map(either => TableReadingError(either.left.get))
      val cells = rights.map(either => either.right.get)

      // Package StringCells and possible errors to output state
      input.copy(errors = input.errors.addError(errors), cells = cells.toIndexedSeq)

    } finally {
      inputStream.close()
    }
  }

  private[csv] def rowTypeDetectionStage(initialInput: State): State = {

    // Accumulate state with row types in the map
    initialInput.cells.foldLeft(initialInput) { (input, cell) =>

      if (rowTyper.isDefinedAt(cell) && !input.rowTypes.contains(cell.rowKey)) {
        rowTyper(cell) match {
          case Left(error) => input.addError(error.relatedCellAdded(cell))
          case Right(rowType) => input.defineRowType(cell.rowKey, rowType)
        }
      } else {
        input
      }

    }
  }

  private[csv] def colTypeDetectionStage(initialInput: State): State = {

    // Accumulate state with column types in the map
    initialInput.cells.foldLeft(initialInput) { (input, cell) =>

      if (colTyper.isDefinedAt(cell, initialInput.rowTypes) && !input.colTypes.contains(cell.colKey)) {
        colTyper(cell, initialInput.rowTypes) match {
          case Left(error) => input.addError(error.relatedCellAdded(cell))
          case Right(colType) => input.defineColType(cell.colKey, colType)
        }
      } else {
        input
      }

    }
  }

  private[csv] def cellUpgradeStage(input: State): State = {
    val upgrader = cellUpgrader.lift

    // Map each cell to same cell, upgraded cell or possible error
    val upgrades = for (cell <- input.cells) yield {

      val upgrade = for (rowType <- input.rowTypes.get(cell.rowKey);
                         colType <- input.colTypes.get(cell.colKey);
                         cellType = CellType[RT, CT](rowType, colType);
                         upgradeResult <- upgrader(cell, cellType)) yield upgradeResult

      upgrade match {
        case Some(Left(tableReadingError)) => Left(tableReadingError.relatedCellAdded(cell))
        case Some(Right(upgradedCell)) => Right(upgradedCell)
        case None => Right(cell)
      }
    }

    val (lefts, rights) = upgrades.partition(_.isLeft)

    val errors = lefts.map(either => either.left.get)
    val upgradedCells = rights.map(either => either.right.get)

    // Package in upgraded cells and possible errors
    input.copy(errors = input.errors.addError(errors), cells = upgradedCells.toIndexedSeq)
  }

}

/**
 * Contains type definitions for various types used in constructing a `TableReader`
 * instance.
 */
object TableReader {

  type StreamOpener = () => java.io.InputStream

  type RowTyperResult[RT] = Either[TableReadingError, RT]

  type RowTyper[RT] = PartialFunction[(Cell), RowTyperResult[RT]]

  type ColTyperResult[CT] = Either[TableReadingError, CT]

  type RowTypes[RT] = SortedBiMap[RowKey, RT]

  type ColTyper[RT, CT] = PartialFunction[(Cell, RowTypes[RT]), ColTyperResult[CT]]

  type ColTypes[CT] = SortedBiMap[ColKey, CT]

  type CellUpgraderResult = Either[TableReadingError, Cell]

  /**
   * A partial function that can inspect cells and their assigned cell types and
   * optionally return a modified (upgraded) cell or [[TableReadingError]]error.
   *
   * Idea is to allow specifying more specialized types like [[fi.pelam.csv.cell.IntegerCell the IntegerCell]]
   * for some cell types.
   *
   * There are two major benefits of more specific cell types. One is that contents of cells get validated
   * better. The other is that getting data from the resulting [[Table]] object later in the client
   * code will be simpler as working with strings can be avoided.
   *
   * @tparam RT client specific row type
   * @tparam CT client specific column type
   */
  type CellUpgrader[RT, CT] = PartialFunction[(Cell, CellType[RT, CT]), CellUpgraderResult]
}
