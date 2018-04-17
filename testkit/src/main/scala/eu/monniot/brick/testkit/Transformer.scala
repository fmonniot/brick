package eu.monniot.brick.testkit

import cats.Monad
import eu.monniot.brick.free.CommandsAlg
import eu.monniot.brick.testkit.TypedMap._

import scala.language.higherKinds
import scala.util.Try

class Transformer[M[_] : Monad] extends CommandsAlg {


  private def asLong(s: String): Try[Long] = Try(s.toLong)

  private def asDouble(s: String): Try[Double] = Try(s.toDouble)


  def transform[A](db: TypedMap[String], op: CommandOp[A]): TypedMap[String] = op match {
    // KEYS
    case Del(keys) => keys.foldLeft(db) { case (redis, key) => redis.remove(key) }
    case Exists(_) => db

    // STRINGS
    case Append(key, value) =>
      db.replace[String](key) {
        case Some(s) => (s + value).typed
        case None => value.typed
      }

    case BitCount(_, None) => db
    case BitCount(_, Some((_, _))) => db

    case BitOp(BitOpOperator.And(keys), dest) => ???

    case BitOp(BitOpOperator.Not(src), dest) => ???

    case BitOp(BitOpOperator.Or(keys), dest) => ???

    case BitOp(BitOpOperator.Xor(keys), dest) => ???

    case BitPos(_, _, Some(_), Some(_)) => db
    case BitPos(_, _, Some(_), None) => db
    case BitPos(_, _, _, _) => db

    case Decr(key) => transform(db, DecrBy(key, 1))
    case DecrBy(key, amount) =>
      db.replace[String](key) {
        case None => s"-$amount".typed
        case Some(str) =>
          asLong(str).map { l => (l - amount).toString }
            .orElse(asDouble(str).map(d => (d - amount).toString))
            .fold(
              t => throw new RuntimeException("Trying to decrement on a non-numeric field", t),
              s => s.typed
            )
      }

    case Get(_) => db
    case GetBit(_, _) => db
    case GetRange(_, _, _) => db
    case GetSet(_, _) => db

    case Incr(key) => transform(db, IncrBy(key, 1))
    case IncrBy(key, amount) =>
      db.replace[String](key) {
        case None => s"$amount".typed
        case Some(str) =>
          asLong(str).map { l => (l + amount).toString }
            .orElse(asDouble(str).map(d => (d + amount).toString))
            .fold(
              t => throw new RuntimeException("Trying to decrement on a non-numeric field", t),
              s => s.typed
            )
      }

    case IncrByFloat(key, amount) =>
      db.replace[String](key) {
        case None => s"$amount".typed
        case Some(str) =>
          asLong(str).map { l => (l + amount).toString }
            .orElse(asDouble(str).map(d => (d + amount).toString))
            .fold(
              t => throw new RuntimeException("Trying to decrement on a non-numeric field", t),
              s => s.typed
            )
      }

    case MGet(_) => db
    case MSet(kvs) =>
      kvs.foldLeft(db) { case (redis, kv) => redis + kv }

    case MSetNx(kvs) =>
      if (kvs.map(_._1).forall(db.haveKey)) db
      else transform(db, MSet(kvs))

    case Set(key, value, expire, exists) => ???

    case SetBit(key, offset, value) => ???

    case SetRange(key, offset, value) => ???

    case StrLen(key) => db

  }

}
