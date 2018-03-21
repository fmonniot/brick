package eu.monniot.brick.testkit


import eu.monniot.brick.testkit.TypedMap.TypedValue

import scala.reflect.runtime.universe._


/**
  * We use reflection here as this class won't be used in production. So we accept to pay the penalty performance.
  *
  * @tparam K The Map's key type
  */
private[testkit] final case class TypedMap[K](private val untyped: Map[K, TypedValue[_]]) {

  import TypedMap._

  //
  // Typed operation
  // When we want to apply an operation on a key with a specific type
  //

  def get[V: TypeTag](key: K): Option[V] =
    untyped.get(key)
      .filter(v => typeTag[V].tpe <:< v.valueType.tpe) // If the value is compatible with the one asked for
      .map(_.value)
      .map(_.asInstanceOf[V])

  def getOrElse[V: TypeTag](key: K, default: V): V = get(key).getOrElse(default)

  def +[V: TypeTag](kv: (K, V)): TypedMap[K] =
    copy(untyped = untyped + ((kv._1, TypedValue(kv._2, typeTag[V]))))

  // This may be expressible in term of fold, filter, map, …
  def replaceAll[V](keys: Seq[K])(f: (K, Option[V]) => TypedValue[V]): TypedMap[K] = {
    TypedMap(keys.foldLeft(untyped) { case (map, key) =>
      val newValue = f(key, get(key))

      map.get(key) match {
        case None => map + ((key, newValue))
        case Some(existing) =>
          // if the existing value is compatible with the new one
          if (existing.valueType.tpe <:< newValue.valueType.tpe) {
            map + ((key, newValue))
          } else {
            map
          }
      }
    })
  }

  def replace[V](key: K)(f: (Option[V]) => TypedValue[V]): TypedMap[K] =
    replaceAll(Seq(key))((_, v: Option[V]) => f(v))

  //
  // Untyped operations.
  // When we want to apply an operation on the key regardless of its type
  //

  def haveKey(key: K): Boolean = untyped.keys.exists(_ == key)

  def remove(key: K): TypedMap[K] = TypedMap(untyped.filterNot { case (k, _) => k == key })

  override def toString: String = s"TypedMap($untyped)"
}

object TypedMap {

  final case class TypedValue[Value](value: Value, valueType: TypeTag[Value]) {
    override def hashCode(): Int = value.hashCode()

    override def equals(obj: scala.Any): Boolean = obj match {
      case _@TypedValue(k2, valueType2) => value == k2 && valueType2.tpe =:= valueType.tpe
      case _ => false
    }
  }

  implicit class ValueOps[V: TypeTag](val value: V) {
    def typed: TypedValue[V] = TypedValue(value, typeTag[V])
  }

}