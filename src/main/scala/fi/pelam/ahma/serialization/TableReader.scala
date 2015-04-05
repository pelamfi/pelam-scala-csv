package fi.pelam.ahma.serialization

import java.nio.charset.{Charset, StandardCharsets}

import com.google.common.io.ByteSource

import scala.collection.mutable.HashMap
import scala.io.Source

class TableReader(input: ByteSource) {

  def read(): Table = {

    null
  }


}

object TableReader {
  type RowAndColCount = (Int, Int)

  def getLines(input: ByteSource, encoding: Charset) = {
    // Bypassing the codec handling in scala.io
    // TODO: Charset detection (try UTF16 and iso8859)
    val source = Source.fromString(input.asCharSource(StandardCharsets.UTF_8).read())

    val lines = source.getLines().toIndexedSeq

    lines
  }

  def getRowAndColCount(keys: TraversableOnce[CellKey]): RowAndColCount = {
    var colMax = 0
    var rowMax = 0
    for (key <- keys) {
      colMax = Math.max(key.colIndex + 1, colMax)
      rowMax = Math.max(key.rowIndex + 1, rowMax)
    }
    (rowMax, colMax)
  }

  def getStringTable(separator: Char, lines: IndexedSeq[String]): HashMap[CellKey, SimpleCell] = {
    val b = HashMap.newBuilder[CellKey, SimpleCell]

    for (line <- lines.zipWithIndex) yield {
      for (cell <- line._1.split(separator).zipWithIndex) yield {
        val key = new CellKey(line._2, cell._2)
        b += key -> new SimpleCell(key, cell._1)
      }
    }

    b.result()
  }

  def getRowTypes(table: Map[CellKey, Cell], rowAndColCount: RowAndColCount): IndexedSeq[RowType] = {
    for (i <- 0 until rowAndColCount._1) yield {
      val key = CellKey(i, 0) // first column
      if (table.contains(key)) {
        val cell = table(key)
        null // identify type
      } else {
        null
      }
    }
  }

}
