package fi.pelam.csv

import scala.collection.SortedMap

/**
 * Bidirectional map supporting multiple values for same key.
 *
 * This class is used in [[CellTypes]] to map between rows and columns and their types.
 *
 * The reverse map is computed lazily as a performance optimization.
 */
// TODO: Could this inherit from SortedMap
case class SortedBiMap[K, V](map: SortedMap[K, V]) extends Map[K, V] {

  import SortedBiMap._
  
  lazy val reverse = reverseMap(map)

  override def updated[B1 >: V](key: K, value: B1): SortedBiMap[K, B1] = SortedBiMap(map.updated(key, value))

  override def +[B1 >: V](kv: (K, B1)): SortedBiMap[K, B1] = SortedBiMap[K, B1](map + kv)

  override def get(key: K): Option[V] = map.get(key)

  override def iterator: Iterator[(K, V)] = map.iterator

  override def -(key: K): SortedBiMap[K, V] = SortedBiMap[K, V](map - key)
}

object SortedBiMap {

  /**
   * Constructor modeled after the one in
   * [[http://www.scala-lang.org/api/current/index.html#scala.collection.generic.SortedMapFactory SortedMapFactory]].
   */
  def apply[K, V](elements: (K, V)*)(implicit ordering: Ordering[K]): SortedBiMap[K, V] =
    SortedBiMap(SortedMap[K, V]() ++ elements)

  /**
   * Utility method for reversing a map. Multiple equal values with different keys
   * are handled by having IndexedSeq with values as the value in the resulting map.
   *
   * Based on idea found on [[http://stackoverflow.com/a/24222250/1148030 StackOverflow]].
   */
  private[csv] def reverseMap[A, B](map: scala.collection.Map[A, B]): Map[B, IndexedSeq[A]] =
    map.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq)
}
