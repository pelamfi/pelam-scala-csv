package fi.pelam.csv.table

import java.io.{BufferedReader, ByteArrayInputStream}
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell._
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.{Pipeline, SortedBiMap}

/**
 *
 * This class is part of the the higher level API for reading, writing and processing CSV data.
 *
 * The result of calling `read` method on this class will be an instance of [[Table]] class.
 * The table is an immutable data structure for holding and processing the parsed data
 * in a spreadsheet program like format.
 *
 * [[TableWriter]] is the counterpart of this class for writing [[Table]] out to disk for example.
 *
 * == Example ==
 *
 * {{{
 * import fi.pelam.csv.cell._
 * import fi.pelam.csv.stream._
 *
 * val reader = TableReader.fromStringSimple(
 *
 *   inputCsv = "name,number\n" +
 *   "foo,1\n" +
 *   "bar,2",
 *
 *   rowTyper = {
 *     case RowKey(0) => "header" // First row is the header
 *     case _ => "data" // and all other rows are "data".
 *   },
 *
 *   colTyper = {
 *     case ColKey(0) => "name"
 *     case ColKey(1) => "number"
 *   },
 *
 *   cellTypeMap = {
 *     case CellType("data", "number") => IntegerCell
 *   })
 *
 * val table = reader.readOrThrow()
 *
 * table.getSingleCol("name", "data").map(_.value).toList
 * // Will give List("foo","bar")
 *
 * table.getSingleCol("number", "data").map(_.value).toList)
 * // Will give List(1,2)
 * }}}
 *
 * == Stages ==
 *
 * The table reading is split into four stages.
 *
 * The table reading process may fail and terminate at each phase. Then an incomplete Table object
 * will be returned together with the errors detected so far.
 *
 * The table reading is split to stages to allow implementing format detection heuristics
 * in a structured manner.
 *
 * - `csvReadingStage` Parse CSV byte data to cells. Depends on `charset` and `separator` provided
 * via the `metadata` parameter.
 *
 * - `rowTypeDetectionStage` Detect row types (hard coded or based on cell contents). The `rowTyper` parameter
 * is used in this stage.
 *
 * - `colTypeDetectionStage` Detect column types (hard coded or based on row types and cell contents). The `colTyper` parameter
 * is used in this stage.
 *
 * - `cellUpgradeStage` Upgrade cells based on cell types, which are combined from row and column types. The `cellUpgrader`
 * parameter is used in this stage
 *
 * == CSV format detection heuristics ==
 *
 * Since deducing whether correct parameters like character set were used in reading a CSV
 * file without any extra knowledge is impossible, this class supports implementing a custom
 * format detection algorithm by client code.
 *
 * The table reading is split to stages to allow implementing format detection heuristics that
 * lock some variables during the earlier stages and then proceeding to later stages.
 *
 * Locking some variables and then proceeding results in more efficient algorithm
 * than exhaustive search of the full set of combinations (character set, locale, separator etc).
 *
 * The actual detection heuristic is handled outside this class. The idea is that the
 * detection heuristic class uses this repeatedly with varying parameters until some critertion
 * is met. The criterion for ending detection could be that zero errors is detected.
 * If no combination of parameters gives zero errors, then the heuristic could just pick
 * the solution which gave errors in the latest stage and then the fewest errors.
 *
 * @param openInputStream A function that returns input stream from which the data should be read.
 *                        The function is called once in `csvReadingStage` and the stream is always closed.
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
// TODO: Add ready made detection heuristics wrappers for this class
class TableReader[RT, CT, M <: TableMetadata](
  val openInputStream: () => java.io.InputStream,
  val metadata: M = SimpleTableMetadata(),
  val rowTyper: TableReader.RowTyper[RT] = PartialFunction.empty,
  val colTyper: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
  val cellUpgrader: TableReader.CellUpgrader[RT, CT] = PartialFunction.empty
  ) {

  type ResultTable = Table[RT, CT, M]

  type State = TableReadingState[RT, CT]

  private[csv] val pipeline = for (_ <- Pipeline.Stage(csvReadingStage);
                      _ <- Pipeline.Stage(rowTypeDetectionStage);
                      _ <- Pipeline.Stage(colTypeDetectionStage);
                      x <- Pipeline.Stage(cellUpgradeStage)) yield x

  def read(): (ResultTable, TableReadingErrors) = {

    val initial = TableReadingState[RT, CT]()

    val result = pipeline.run(initial)

    (Table(result.cells, result.rowTypes, result.colTypes, metadata), result.errors)
  }

  private[csv] def csvReadingStage(input: State): State = {
    val inputStream = openInputStream()

    try {

      val inputReader: java.io.Reader = new BufferedReader(new java.io.InputStreamReader(inputStream, metadata.charset), 1024)

      val csvReader = new CsvReader(inputReader, separator = metadata.separator)

      val (lefts, rights) = csvReader.partition(_.isLeft)

      val errors = lefts.map(either => TableReadingError(either.left.get))
      val cells = rights.map(either => either.right.get)

      // Package StringCells and possible errors to output state
      input.copy(errors = input.errors.add(errors), cells = cells.toIndexedSeq)

    } finally {
      inputStream.close()
    }
  }

  private[csv] def rowTypeDetectionStage(initialInput: State): State = {

    // Accumulate state with row types in the map
    initialInput.cells.foldLeft(initialInput) { (input, cell) =>

      if (rowTyper.isDefinedAt(cell) && !input.rowTypes.contains(cell.rowKey)) {
        rowTyper(cell) match {
          case Left(error) => input.addError(error.addedDetails(cell))
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
          case Left(error) => input.addError(error.addedDetails(cell))
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
        case Some(Left(tableReadingError)) => Left(tableReadingError.addedDetails(cell))
        case Some(Right(upgradedCell)) => Right(upgradedCell)
        case None => Right(cell)
      }
    }

    val (lefts, rights) = upgrades.partition(_.isLeft)

    val errors = lefts.map(either => either.left.get)
    val upgradedCells = rights.map(either => either.right.get)

    // Package in upgraded cells and possible errors
    input.copy(errors = input.errors.add(errors), cells = upgradedCells.toIndexedSeq)
  }

  /**
   * This method extends the usual read method with exception based error handling, which
   * may be useful in smaller applications that don't expect errors in input.
   *
   * A `RuntimeException` will be thrown when error is encountered.
   *
   */
  def readOrThrow(): ResultTable = {
    val (table, errors) = read()
    if (errors.noErrors) {
      table
    } else {
      sys.error(errors.toString)
    }
  }

}

object TableReader {

  /**
   * Alternate constructor for CsvReader providing string input.
   *
   * This alternate constructor exists mainly to make tests and code examples shorter.
   */
  def fromString[RT, CT, M <: TableMetadata](
    inputString: String,
    metadata: M = SimpleTableMetadata(),
    rowTyper: TableReader.RowTyper[RT] = PartialFunction.empty,
    colTyper: TableReader.ColTyper[RT, CT] = PartialFunction.empty,
    cellUpgrader: TableReader.CellUpgrader[RT, CT] = PartialFunction.empty) = {

    new TableReader(() => new ByteArrayInputStream(
      inputString.getBytes(CsvConstants.defaultCharset)),
      metadata,
      rowTyper,
      colTyper,
      cellUpgrader)
  }

  /**
   * A helper method to build a [[Table]] from a CSV string and
   * providing simplified row and column typers using only
   * [[fi.pelam.csv.cell.RowKey RowKey]] and
   * [[fi.pelam.csv.cell.ColKey ColKey]] as input.
   *
   * This alternate constructor exists mainly to make tests and code examples shorter.
   */
  // TODO: Better name, where does this example helper belong?
  def fromStringSimple[RT, CT, M <: TableMetadata](
    inputCsv: String,
    metadata: M = SimpleTableMetadata(),
    rowTyper: PartialFunction[(RowKey), RT] = PartialFunction.empty,
    colTyper: PartialFunction[(ColKey), CT] = PartialFunction.empty,
    cellTypeMap: PartialFunction[CellType[_, _], CellParser] = PartialFunction.empty,
    cellParsingLocale: Locale = Locale.ROOT) = {

    // TODO: Is this too complex just to read table and assign types
    val rowTyperWrapped: RowTyper[RT] = {
      case (cell: Cell) if rowTyper.isDefinedAt(cell.rowKey) => Right(rowTyper(cell.rowKey))
    }

    val colTyperWrapped: ColTyper[RT, CT] = {
      case (cell: Cell, _) if colTyper.isDefinedAt(cell.colKey) => Right(colTyper(cell.colKey))
    }

    val cellUpgrader = defineCellUpgrader[RT, CT](cellParsingLocale, cellTypeMap)

    new TableReader(() => new ByteArrayInputStream(
      inputCsv.getBytes(CsvConstants.defaultCharset)),
      metadata,
      rowTyperWrapped,
      colTyperWrapped,
      cellUpgrader)
  }


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

  /**
   * This is a helper method to setup a simple cell upgrader
   * from a map of [[CellType CellTypes]] and [[fi.pelam.csv.cell.CellParser CellParsers]].
   *
   * @param locale locale to be passed to cell parsers
   * @param parserMap a map from [[CellType CellTypes]] to [[fi.pelam.csv.cell.CellParser CellParsers]]
   * @tparam RT client specific row type
   * @tparam CT client specific column type
   * @return a [[CellUpgrader]] to be passed to [[TableReader]]
   */
  def defineCellUpgrader[RT, CT](locale: Locale, parserMap: PartialFunction[CellType[_, _], CellParser]): CellUpgrader[RT, CT] = {

    case (cell, cellType) if parserMap.isDefinedAt(cellType) => {

      parserMap(cellType).parse(cell.cellKey, locale, cell.serializedString) match {
        case Left(error) => Left(TableReadingError(error, cell, cellType))
        case Right(cell) => Right(cell)
      }
    }

  }
}