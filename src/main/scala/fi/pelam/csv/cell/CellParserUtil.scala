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

package fi.pelam.csv.cell

import java.util.Locale

object CellParserUtil {

  def addLocaleToError(parser: Cell.Parser, locale: Locale): Cell.Parser = {
    { (cellKey: CellKey, input: String) =>
      parser(cellKey, input) match {
        case Left(e: CellParsingError) => Left(e.withExtraMessage(s"Used locale '$locale'."))
        case Right(x)=> Right(x)
      }
    }
  }
}
