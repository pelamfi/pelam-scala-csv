# Pelam's Scala CSV Library

[![Build Status](https://travis-ci.org/pelamfi/pelam-scala-csv.svg?branch=master)](https://travis-ci.org/pelamfi/pelam-scala-csv)

Scala library for reading and writing CSV data with an optional high level
API.

This project is still under development and the APIs may change. It
is however functional and tested with the included test suite so feel free
to use it. Please send me feedback on bugs and features you find useful,
problematic or missing.

## ScalaDoc

[A recent build of the ScalaDoc is available here.](https://s3.amazonaws.com/pelam-scala-csv-doc/pelamfi/pelam-scala-csv/17/17.1/home/travis/build/pelamfi/pelam-scala-csv/build/docs/scaladoc/index.html#fi.pelam.csv.package)

## Main points

There are other CSV reading libraries and writing a CSV parser from scratch is not 
very difficult.

However, the specialty of this library is the higher level "table oriented" API.

The higher level API helps in processing complex spreadsheets as well as detecting and handling
differences in the CSV syntax. Such differences are common because the CSV is not a
standardized format.

  * Two levels of APIs for both input and output
  * First API is a simple streaming api for converting a CSV file to and from a stream of StringCell objects.
    * Reading a CSV file is done with a Scala Iterator interface
  * Second higher level API is richer and is based on the Table class.
    * Supports attaching type information to rows and columns
    * Allows having some regions represented with smarter and custom Cell types 
      (like your own date cell.)
    * Autodetection of CSV format details
    * Pluggable functions for defining Row, Column and Cell types.
    * Supports management of locales (Office apps change default CSV
      format based on locale.)


## License

Pelam's Scala CSV library is distributed under the 
Apache 2.0 license which is available in the included file [LICENSE.txt](LICENSE.txt)
and [online](http://www.apache.org/licenses/LICENSE-2.0).


## Maven coordinates

TBD

## Quick examples

### Stream parsing

This is the most basic way of accessing CSV data. See later examples for a more 
structured approach. The parsing is done through Scala's `Iterator` API in a streaming fashion.

This example code will pick the second column from the `csvData` and print the `toString`
of each `Cell` from that column. Note the use of `throwOnError` to bypass the `Either`
normally returned by the reader.
 
```scala

   import fi.pelam.csv.cell._
   import fi.pelam.csv.stream._

   val csvData =
       "apple,0.99,3\n" +
       "orange,1.25,2\n" +
       "banana,0.80,4\n"

   val pickedCol = ColKey(1)

   for (cell <- new CsvReader(csvData).throwOnError; if cell.colKey == pickedCol) {
     println(cell)
   }

   // Running the code above will print the cells from the second second column:
   Cell containing '0.99' at Row 1, Column B (1)
   Cell containing '1.25' at Row 2, Column B (1)
   Cell containing '0.80' at Row 3, Column B (1)
```

### Parsing to a table object

This example demonstrates the higher level API. The CSV is parsed into a 
`Table` object that provides various methods for accessing regions of 
the table. Also cells containing numeric data use appropriate `Cell` sub type.

```scala

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
        case CellType("data", "price") => Dou
        bleCell.defaultParser
      }))

    // Get values from cells in column with type "product" on rows with type "data."
    table.getSingleCol("data", "product").map(_.value).toList
    // Will give List("apple", "orange", "banana")

    // Get values from cells in column with type "number" on rows with type "data."
    table.getSingleCol("number", "data").map(_.value).toList)
    // Will give List(0.99, 1.25, 0.8)

```

### Parsing with a format detection heuristic

This example builds upon table object parsing in previous example, but adds a 
heuristic format detection process on top of it. This may be useful in applications 
that need to deal with CSV exported from different Spreadsheet programs with 
different locale settings.

```scala
    import fi.pelam.csv.table._
    import fi.pelam.csv.cell._
    import TableReaderConfig._

    val validColTypes = Set("header", "model", "price")

    // Setup a DetectingTableReader which will try combinations of CSV formatting types
    // to understand the data.
    val reader = DetectingTableReader[String, String](

      tableReaderMaker = { (metadata) => new TableReader(

        // An implicit from the object TableReaderConfig converts the string
        // to a function providing streams.
        openStream =
          "header;model;price\n" +
          "data;300D;1,234.0\n" +
          "data;SLS AMG;234,567.89",

        // Make correct metadata end up in the final Table
        tableMetadata = metadata,

        // First column specifies row types
        rowTyper = makeRowTyper({
          case (CellKey(_, 0), rowType) => rowType
        }),

        // Column type is specified by the first row.
        // Type names are checked and error is generated for unknown
        // column types by errorOnUndefinedCol.
        // This strictness is what enables the correct detection of CSV format.
        colTyper = errorOnUndefinedCol(makeColTyper({
          case (CellKey(0, _), colType) if validColTypes.contains(colType) => colType
        })),

        cellUpgrader = makeCellUpgrader({
          case CellType("data", "price") => DoubleCell.parserForLocale(metadata.dataLocale)
        }))
      }
    )

    val table = reader.readOrThrow()

    // Get values from cells in column with type "name" on rows with type "data."
    table.getSingleCol("data", "model").map(_.value).toList
    // Will give List("300D", "SLS AMG")

    // Get values from cells in column with type "number" on rows with type "data."
    table.getSingleCol("number", "price").map(_.value).toList)
    // Will give List(1234, 234567.89)
```

## Demo application

TBD

## TODO list

  * TODO: Some simple demo project
  * TODO: Mark classes and objects not intended to be extended final.
  * TODO: Try to reduce dependencies of the final artifact.
  * TODO: Some performance tests and profiling to at least have some idea of possible performance disasters.
  * TODO: Test the streaming api with massive data and document if it works.
  * TODO: A Gradle task to publish the built scaladoc to S3 into a sensible directory.

## History of this project

This CSV code was originally developed as an IO solution for a custom project called Ahma.
I broke it off from Ahma as I felt that this had a tiny chance of becoming a generally useful
open source Scala library. The history up to around June 28th 2015 is pretty broken (won't compile etc.) due
to filtering of the git history. However what remains may still be useful for me at least if I need
to recover my original rationale for some detail.


