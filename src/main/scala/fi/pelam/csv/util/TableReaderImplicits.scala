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

/**
 * A set of implicit functions that map various things used as parameters for
 * [[fi.pelam.csv.table.TableReader]].
 *
 * Idea is to allow various simpler ways of configuring the `TableReader`.
 *
 */
// TODO: Move this object to .table package due to circular deps
object TableReaderImplicits{

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



}
