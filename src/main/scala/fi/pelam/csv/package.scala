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

package fi.pelam

/**
 * This is pelam-scala-csv contained in package fi.pelam.csv. Pelam-scala-csv is a
 * library for reading and writing CSV data.
 * Two levels of APIs are provided for both input and output
 *
 * == main points ==
 *
 *  - [[fi.pelam.csv.stream.CsvReader The first API]] is a simple streaming api for converting a CSV file to and from a stream of StringCell objects.
 *  - Reading a CSV file is done with a Scala Iterator interface
 *
 *  - [[fi.pelam.csv.table.TableReader Second API]] is richer and is based on a more high level Table object.
 *  - Contains some type information on rows and columns
 *  - Allows having some regions represented with smarter and custom Cell types (like your own date cell)
 *  - Autodetection of CSV format details
 *  - Pluggable method for defining Row, Column and Cell types.
 *  - Supports management of locales (Office apps change default CSV format based on locale.)
 *
 */
// TODO: Link more classes in this package scaladoc
package object csv {

}
