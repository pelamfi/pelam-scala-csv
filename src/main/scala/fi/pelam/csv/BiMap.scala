package fi.pelam.csv

case class BiMap[K, V](map: scala.collection.SortedMap[K, V]) extends Map[K, V] {
  
  lazy val reverse = Table.reverseMap(map)

  override def updated[B1 >: V](key: K, value: B1): BiMap[K, B1] = BiMap(map.updated(key, value))

  override def +[B1 >: V](kv: (K, B1)): Map[K, B1] = BiMap[K, B1](map + kv)

  override def get(key: K): Option[V] = map.get(key)

  override def iterator: Iterator[(K, V)] = map.iterator

  override def -(key: K): Map[K, V] = BiMap[K, V](map - key)
}
