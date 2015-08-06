Pelam's Scala CSV Library
=========================
[![Build Status](https://travis-ci.org/pelamfi/pelam-scala-csv.svg?branch=master)](https://travis-ci.org/pelamfi/pelam-scala-csv)

NOTE: THIS PROJECT IS NOT YET FUNCTIONAL.
See History section below...

Scala library for reading and writing CSV data. 

Main points
===========

  * Two levels of APIs for both input and output


  * First API is a simple streaming api for converting a CSV file to and from a
    stream of StringCell objects.
    * Reading a CSV file is done with a Scala Iterator interface


  * Second API is richer and is based on a more high level Table object.
    * Contains some type information on rows and columns
    * Allows having some regions represented with smarter and custom Cell types 
      (like your own date cell)
    * Autodetection of CSV format details
    * Pluggable method for defining Row, Column and Cell types.
    * Supports management of locales (Office apps change default CSV
      format based on locale.)


License
=======

TODO, some permissive license

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

History of this project
=======================

This CSV code was originally developed as an IO solution for a custom project called Ahma.
I broke it off from Ahma as I felt that this had a tiny chance of becoming a generally useful
open source Scala library. The history up to around June 28th 2015 is pretty broken (won't compile etc.) due
to filtering of the git history. However what remains may still be useful for me at least if I need
to recover my original rationale for some detail.


