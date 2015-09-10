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

import java.io.ByteArrayInputStream
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell.{Cell, CellKey}

/**
 * A set of functions that map various things used as parameters for
 * [[fi.pelam.csv.table.TableReader]].
 *
 * Idea is to allow various simpler ways of configuring the `TableReader`.
 *
 */
// TODO: Document methods with examples
object TableReaderConfig {

  implicit def stringToStream(string: String): () => java.io.InputStream = {
    () => new ByteArrayInputStream(string.getBytes(CsvConstants.defaultCharset))
  }

  def errorOnUndefinedRow[RT](typer: TableReader.RowTyper[RT]): TableReader.RowTyper[RT] = {
    case cell if typer.isDefinedAt(cell) => typer(cell)
    case _ => Left(TableReadingError(s"Undefined row type."))
  }

  def errorOnUndefinedCol[RT, CT](typer: TableReader.ColTyper[RT, CT]): TableReader.ColTyper[RT, CT] = {
    case cell if typer.isDefinedAt(cell) => typer(cell)
    case _ => Left(TableReadingError(s"Undefined column type."))
  }

  def makeRowTyper[RT](rowTyper: PartialFunction[(CellKey, String), RT]): TableReader.RowTyper[RT] = {

    val rowTyperWrapped: TableReader.RowTyper[RT] = {
      case (cell: Cell) if rowTyper.isDefinedAt(cell.cellKey, cell.serializedString) => Right(rowTyper(cell.cellKey, cell.serializedString))
    }

    rowTyperWrapped
  }

  def makeColTyper[RT, CT](colTyper: PartialFunction[(CellKey, String), CT]) = {
    val colTyperWrapped: TableReader.ColTyper[RT, CT] = {
      case (cell: Cell, _) if colTyper.isDefinedAt(cell.cellKey, cell.serializedString) => Right(colTyper(cell.cellKey, cell.serializedString))
    }
    colTyperWrapped
  }

  /**
   * This is a helper method to setup a simple cell upgrader
   * for [[fi.pelam.csv.table.TableReader]] from a map of
   * [[CellType CellTypes]] to [[fi.pelam.csv.cell.Cell.Parser Cell Parsers]].
   *
   * @param locale locale to be passed to cell parsers
   * @param parserMap a map from [[CellType CellTypes]] to [[fi.pelam.csv.cell.Cell.Parser CellParsers]]
   * @tparam RT client specific row type
   * @tparam CT client specific column type
   * @return a [[TableReader.CellUpgrader]] to be passed to [[TableReader]]
   */
  def makeCellUpgrader[RT, CT](parserMap: PartialFunction[CellType[_, _], Cell.Parser], locale: Locale = Locale.ROOT): TableReader.CellUpgrader[RT, CT] = {
    case (cell, cellType) if parserMap.isDefinedAt(cellType) => {
      val parse = parserMap(cellType)

      parse(cell.cellKey, cell.serializedString) match {
        case Left(error) => Left(TableReadingError(error, cell, cellType))
        case Right(cell) => Right(cell)
      }
    }

  }


}
