package fi.pelam.csv.table

import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell._
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.SortedBiMap

object TableReader2 {

  type RowTyperResult[RT] = Either[TableReadingError, RT]

  type RowTyper[RT] = PartialFunction[(Cell), RowTyperResult[RT]]

  type ColTyperResult[CT] = Either[TableReadingError, CT]

  type RowTypes[RT] = SortedBiMap[RowKey, RT]

  type ColTyper[RT, CT] = PartialFunction[(Cell, RowTypes[RT]), ColTyperResult[CT]]

  type ColTypes[CT] = SortedBiMap[ColKey, CT]

  type CellUpgraderResult = Either[TableReadingError, Cell]

  type CellUpgrader[RT, CT] = PartialFunction[(Cell, CellType[RT, CT]), CellUpgraderResult]

  def defineCellUpgrader[RT, CT](locale: Locale, parserMap: Map[CellType[_, _], CellParser]): CellUpgrader[RT, CT] = {

    case (cell, cellType) if parserMap.contains(cellType) => {

      parserMap(cellType).parse(cell.cellKey, locale, cell.serializedString) match {
        case Left(error) => Left(TableReadingError(error, cell, cellType))
        case Right(cell) => Right(cell)
      }
    }

  }
}

case class TableReadingErrors(phase: Int = 0, errors: IndexedSeq[TableReadingError] = IndexedSeq()) {

  def add(moreErrors: TraversableOnce[TableReadingError]): TableReadingErrors = copy(errors = errors ++ moreErrors.toIndexedSeq)

  def add(error: TableReadingError): TableReadingErrors = copy(errors = errors :+ error)

  def noErrors = errors.isEmpty 
}

case class TableReadingState[RT, CT](cells: IndexedSeq[Cell] = IndexedSeq(),
  rowTypes: TableReader2.RowTypes[RT] = SortedBiMap[RowKey, RT](),
  colTypes: TableReader2.ColTypes[CT] = SortedBiMap[ColKey, CT](),
  errors: TableReadingErrors = TableReadingErrors()) {

  def defineRowType(row: RowKey, rowType: RT): TableReadingState[RT, CT] = copy(rowTypes = rowTypes.updated(row, rowType))

  def defineColType(col: ColKey, colType: CT): TableReadingState[RT, CT] = copy(colTypes = colTypes.updated(col, colType))

  def addErrors(moreErrors: Iterator[TableReadingError]): TableReadingState[RT, CT] = copy(errors = errors.add(moreErrors))

  def addError(error: TableReadingError): TableReadingState[RT, CT] = copy(errors = errors.add(error))
}

sealed trait TableReadingPhase[RT, CT] {
  def map(f: TableReadingState[RT, CT] => TableReadingState[RT, CT]): TableReadingPhase[RT, CT]
  def flatMap(inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT]
  def run(inputState: TableReadingState[RT, CT]): TableReadingState[RT, CT]
}

case class Phase[RT, CT](phaseFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) extends TableReadingPhase[RT, CT] {

  def map(mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = Phase[RT, CT](state => mapFunc(phaseFunc(state)))

  def flatMap(inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT] = PhaseFlatmap(this, inner)

  def run(inputState: TableReadingState[RT, CT]) = phaseFunc(inputState)
}

case class PhaseFlatmap[RT, CT](outer: TableReadingPhase[RT, CT],
  inner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT],
  mapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT] = (state: TableReadingState[RT, CT]) => state) extends TableReadingPhase[RT, CT] {

  def map(newMapFunc: TableReadingState[RT, CT] => TableReadingState[RT, CT]) = PhaseFlatmap(outer, inner,
    (state: TableReadingState[RT, CT]) => mapFunc(newMapFunc(state)))

  def flatMap(newInner: TableReadingState[RT, CT] => TableReadingPhase[RT, CT]): TableReadingPhase[RT, CT] = PhaseFlatmap(this, newInner)

  def run(inputState: TableReadingState[RT, CT]) = {
    val outerResult = outer.run(inputState)
    if (outerResult.errors.noErrors) {
      mapFunc(inner(outerResult).run(outerResult)) // to call the inner func, we need state and we get the wrapper,
    } else {
      // Don't run further phases if one had error
      mapFunc(outerResult)
    }
  }
}

/**
 * This class is part of the the higher level api for reading, writing and processing CSV data.
 *
 * [[Table]] is an immutable data structure for holding and processing
 * the parsed data in a spreadsheet program like format.
 *
 * [[TableWriter]] is the counterpart of this class for writing [[Table]] out to disk.
 *
 * == Stages ==
 * The table reading process may fail and terminate at each phase. Then an incomplete Table object
 * will be returned containing the errors detected so far.
 *
 * The table reading is split to phases to allow implementing format detection heuristics in that
 * lock some variables during the earlier phases and then proceeding to later phases.
 *
 * Locking some variables and then proceeding results in more efficient algorithm
 * than exhaustive search of the full set of combinations (character set, locale, separator etc).
 *
 *   - Parse to cells
 *   - Detect cell types
 *   - Upgrade cells
 *
 * @param openInputStream
 * @param rowTypeDefinition
 * @param colTypeDefinition
 * @param cellTypes map from [[CellType]] to [[fi.pelam.csv.cell.CellParser CellParser]] instances. Use this to get more
 *                  specialized [[fi.pelam.csv.cell.Cell Cell]] instances than the simple
 *                  [[fi.pelam.csv.cell.StringCell StringCell]].
 * @param locales
 * @tparam RT
 * @tparam CT
 */
// TODO: Update docs wrt. new TableReader design
// TODO: Finish documenting the phases and the detection idea after it is implemented
class TableReader2[RT, CT, M <: TableMetadata](
  val openInputStream: () => java.io.InputStream,
  val metadata: M = SimpleTableMetadata(),
  val rowTyper: TableReader2.RowTyper[RT] = PartialFunction.empty,
  val colTyper: TableReader2.ColTyper[RT, CT] = PartialFunction.empty,
  val cellUpgrader: TableReader2.CellUpgrader[RT, CT] = PartialFunction.empty
  ) {

  import TableReader2._

  type ResultTable = Table[RT, CT, M]

  type State = TableReadingState[RT, CT]

  def csvReadingPhase(input: State): State = {
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

  def rowTypeDetectionPhase(initialInput: State): State = {

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

  def colTypeDetectionPhase(initialInput: State): State = {

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

  def cellUpgradePhase(input: State): State = {
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

  def read(): (ResultTable, TableReadingErrors) = {

    val phases = for (_ <- Phase(csvReadingPhase);
                      _ <- Phase(rowTypeDetectionPhase);
                      _ <- Phase(colTypeDetectionPhase);
                      x <- Phase(cellUpgradePhase)) yield x

    val initial = TableReadingState[RT, CT]()

    val result = phases.run(initial)

    val cellTypes = CellTypes[RT, CT](result.rowTypes, result.colTypes)

    (Table(metadata, cellTypes, result.cells), result.errors)
  }
}

