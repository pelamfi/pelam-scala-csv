package fi.pelam

import fi.pelam.csv.table.TableReader

/**
 * This is pelam-scala-csv contained in package fi.pelam.csv. Pelam-scala-csv is a
 * library for reading and writing CSV data.
 * Two levels of APIs are provided for both input and output
 *
 * == main points ==
 *
 *  - [[fi.pelam.csv.stream.CsvReader The first API]] is a simple streaming api for converting a CSV file to and from a stream of StringCell objects.
 *   - Reading a CSV file is done with a Scala Iterator interface
 *  - [[fi.pelam.csv.table.TableReader Second API]] is richer and is based on a more high level Table object.
 *   - Contains some type information on rows and columns
 *   - Allows having some regions represented with smarter and custom Cell types (like your own date cell)
 *   - Autodetection of CSV format details
 *   - Pluggable method for defining Row, Column and Cell types.
 *   - Supports management of locales (Office apps change default CSV format based on locale.)
 *
 */
// TODO: Link more classes in this package scaladoc
package object csv {

}
