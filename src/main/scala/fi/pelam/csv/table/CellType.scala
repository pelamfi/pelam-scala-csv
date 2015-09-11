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

/**
 * The "type" of a cell in a [[Table Table]] is considered a pair of objects each
 * identifying the row type and the column type respectively.
 * <p>
 * This type concept should not be confused with general scala types. Row and column
 * types are typically just some suitable (case) objects defined by the client code.
 * <p>
 * Cell types are used in [[TableReader]] to map which actual subtypes of [[fi.pelam.csv.cell.Cell Cell]] should
 * be used for each position in table.
 *
 * @param rowType instance of row type defined by client code.
 * @param colType instance of column type defined by client code.
 * @tparam RT the type of all row types used by the client code.
 * @tparam CT the type of all column types used by the client code.
 */
final case class CellType[RT, CT](rowType: RT, colType: CT) {

}
