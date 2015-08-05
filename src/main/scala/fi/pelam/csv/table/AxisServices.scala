package fi.pelam.csv.table
import fi.pelam.csv.cell.{AxisKey, Cell, ColKey, RowKey}
import fi.pelam.csv.util.SortedBiMap

/**
 * This class is used internally by this CSV package to track mapping of rows and columns to their
 * user defined type objects. Also includes some other things that maybe should be elsewhere.
 *
 * Common services based on map describing either row types or column types.
 */
abstract class AxisServices[K <: AxisKey[K], T](types: SortedBiMap[K, T] = SortedBiMap[K, T]()) {
  import AxisServices._

  /**
   * Largest key
   */
  val keyRangeEnd = findKeyRangeEnd(types.keys)

  def getType(cell: Cell): Option[T] = {
    for (colType <- types.get(getAxisKey(cell))) yield colType
  }

  /**
   * Get corresponding [[AxisKey]] from [[Cell]]
   */
  def getAxisKey(cell: Cell): K

  /**
   * "row" or "column"
   */
  val name: String

  def keysByType = types.reverse

  /**
   * Throws if the number of columns/rows with given type is not 1
   */
  def getSingleKeyByType(colType: T): K = {
    val keys = keysByType(colType)
    if (keys.size == 0) {
      sys.error(s"Expected 1 $name of type $colType but no $name of that type found.")
    } else if (keys.size > 1) {
      sys.error(s"Expected 1 $name of type $colType but more than 1 found.")
    } else {
      keys(0)
    }
  }
}

object AxisServices{
  def findKeyRangeEnd(keys: TraversableOnce[AxisKey[_]]) = keys.foldLeft(0)((max, key) => Math.max(max, key.index + 1))
}
