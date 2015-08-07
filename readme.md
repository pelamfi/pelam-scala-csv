Pelam's Scala CSV Library
=========================
[![Build Status](https://travis-ci.org/pelamfi/pelam-scala-csv.svg?branch=master)](https://travis-ci.org/pelamfi/pelam-scala-csv)

Scala library for reading and writing CSV data with an optional high level
API.

This project is still under development and the APIs may change. It
is however functional and tested with the included test suite so feel free
to use it. Please send me feedback on bugs and features you find useful,
problematic or missing.

ScalaDoc
========
[A recent build of the ScalaDoc is available here.](https://s3.amazonaws.com/pelam-scala-csv-doc/pelamfi/pelam-scala-csv/17/17.1/home/travis/build/pelamfi/pelam-scala-csv/build/docs/scaladoc/index.html#fi.pelam.csv.package)

Main points
===========
To be clear there are other CSV reading libraries and even writing a CSV parser from scratch is not
very difficult.

However the specialty of this library is the higher level "table oriented" API.
The higher level API helps in processing complex spreadsheets as well as detecting and handling
differences in low level CSV syntax which often arise because the CSV is not a standardized or
very structured format.

  * Two levels of APIs for both input and output


  * First API is a simple streaming api for converting a CSV file to and from a
    stream of StringCell objects.
    * Reading a CSV file is done with a Scala Iterator interface


  * Second higher level API is richer and is based on the Table class.
    * Supports attaching type information to rows and columns
    * Allows having some regions represented with smarter and custom Cell types 
      (like your own date cell.)
    * Autodetection of CSV format details
    * Pluggable functions for defining Row, Column and Cell types.
    * Supports management of locales (Office apps change default CSV
      format based on locale.)


License
=======

Pelam's Scala CSV library is distribute under the 
Apache 2.0 license which is available in the included file [LICENSE.txt](LICENSE.txt)
and [online](http://www.apache.org/licenses/LICENSE-2.0).


Maven coordinates
=================

TBD

Quick examples
==============

TBD

Demo application
================

TBD

TODO list
=========

  * TODO: The locale detection in TableReader is a forced on feature. 
  The interface should allow either providing other similar algorithms or just remove the thing.
  * TODO: Some simple demo project
  * TODO: Mark classes and objects not intended to be extended final.
  * TODO: Add @constructor scaladoc tags where needed in class scaladocs.
  * TODO: Try to reduce dependencies of the final artifact.
  * TODO: Some performance tests and profiling to at least have some idea of possible performance disasters.
  * TODO: Test the streaming api with massive data and document if it works.
  * TODO: A Gradle task to publish the built scaladoc to S3 into a sensible directory.

History of this project
=======================

This CSV code was originally developed as an IO solution for a custom project called Ahma.
I broke it off from Ahma as I felt that this had a tiny chance of becoming a generally useful
open source Scala library. The history up to around June 28th 2015 is pretty broken (won't compile etc.) due
to filtering of the git history. However what remains may still be useful for me at least if I need
to recover my original rationale for some detail.


