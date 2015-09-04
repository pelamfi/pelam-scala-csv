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

package fi.pelam.csv.util

import java.io.ByteArrayInputStream

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell.{CellKey, ColKey, Cell, RowKey}
import fi.pelam.csv.table.{TableReadingError, TableMetadata, TableReader}
import java.io.{InputStream, BufferedReader, ByteArrayInputStream}
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell._
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.{Pipeline, SortedBiMap}
import java.io.{InputStream, BufferedReader, ByteArrayInputStream}
import java.util.Locale

import fi.pelam.csv.CsvConstants
import fi.pelam.csv.cell._
import fi.pelam.csv.stream.CsvReader
import fi.pelam.csv.util.{Pipeline, SortedBiMap}
import fi.pelam.csv.cell._
import fi.pelam.csv.table._

/**
 * A set of implicit functions that map various things used as parameters for
 * [[fi.pelam.csv.table.TableReader]].
 *
 * Idea is to allow various simpler ways of configuring the `TableReader`.
 *
 */
// TODO: Move this object to .table package due to circular deps, give better name
object TableReaderImplicits {

  implicit def stringToStream(string: String): () => java.io.InputStream = {
    () => new ByteArrayInputStream(string.getBytes(CsvConstants.defaultCharset))
  }

  implicit def errorOnUndefinedRow[RT](typer: TableReader.RowTyper[RT]): TableReader.RowTyper[RT] = {
    case cell if typer.isDefinedAt(cell) => typer(cell)
    case _ => Left(TableReadingError(s"Undefined row type"))
  }

  implicit def errorOnUndefinedCol[RT, CT](typer: TableReader.ColTyper[RT, CT]): TableReader.ColTyper[RT, CT] = {
    case cell if typer.isDefinedAt(cell) => typer(cell)
    case _ => Left(TableReadingError(s"Undefined column type"))
  }

  implicit def makeRowTyper[RT](rowTyper: PartialFunction[(CellKey, String), RT]): TableReader.RowTyper[RT] = {

    val rowTyperWrapped: TableReader.RowTyper[RT] = {
      case (cell: Cell) if rowTyper.isDefinedAt(cell.cellKey, cell.serializedString) => Right(rowTyper(cell.cellKey, cell.serializedString))
    }

    rowTyperWrapped
  }

  implicit def makeColTyper[RT, CT](colTyper: PartialFunction[(CellKey, String), CT]) = {
    val colTyperWrapped: TableReader.ColTyper[RT, CT] = {
      case (cell: Cell, _) if colTyper.isDefinedAt(cell.cellKey, cell.serializedString) => Right(colTyper(cell.cellKey, cell.serializedString))
    }
    colTyperWrapped
  }

  /*
  implicit def makeColTyper[RT, CT](colTyper: PartialFunction[(CellKey, String), TableReader.ColTyperResult[CT]]) = {
    val colTyperWrapped: TableReader.ColTyper[RT, CT] = {
      case (cell: Cell, _) if colTyper.isDefinedAt(cell.cellKey, cell.serializedString) => colTyper(cell.cellKey, cell.serializedString)
    }
    colTyperWrapped
  }*/


  /**
   * This is a helper method to setup a simple cell upgrader
   * from a map of [[CellType CellTypes]] and [[CellParser CellParsers]].
   *
   * @param locale locale to be passed to cell parsers
   * @param parserMap a map from [[CellType CellTypes]] to [[fi.pelam.csv.cell.CellParser CellParsers]]
   * @tparam RT client specific row type
   * @tparam CT client specific column type
   * @return a [[TableReader.CellUpgrader]] to be passed to [[TableReader]]
   */
  def makeCellUpgrader[RT, CT](parserMap: PartialFunction[CellType[_, _], CellParser], locale: Locale = Locale.ROOT): TableReader.CellUpgrader[RT, CT] = {
    case (cell, cellType) if parserMap.isDefinedAt(cellType) => {

      parserMap(cellType).parse(cell.cellKey, locale, cell.serializedString) match {
        case Left(error) => Left(TableReadingError(error, cell, cellType))
        case Right(cell) => Right(cell)
      }
    }

  }


}
