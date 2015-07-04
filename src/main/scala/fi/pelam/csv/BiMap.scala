package fi.pelam.csv

case class BiMap[K, V](map: scala.collection.SortedMap[K, V]) extends Map[K, V] {

  import BiMap._
  
  lazy val reverse = reverseMap(map)

  override def updated[B1 >: V](key: K, value: B1): BiMap[K, B1] = BiMap(map.updated(key, value))

  override def +[B1 >: V](kv: (K, B1)): Map[K, B1] = BiMap[K, B1](map + kv)

  override def get(key: K): Option[V] = map.get(key)

  override def iterator: Iterator[(K, V)] = map.iterator

  override def -(key: K): Map[K, V] = BiMap[K, V](map - key)
}

object BiMap {
  // http://stackoverflow.com/a/24222250/1148030
  def reverseMap[A, B](map: scala.collection.Map[A, B]): Map[B, IndexedSeq[A]] = map.groupBy(_._2).mapValues(_.map(_._1).toIndexedSeq)
}
