package eu.monniot.fs2.redis.free

import cats.data.NonEmptyList
import cats.free.Free.liftF
import eu.monniot.fs2.redis.free.commands._

import scala.concurrent.duration.FiniteDuration


trait StringCommands {

  def append(key: String, value: String): CommandIO[Long] =
    liftF(Append(key, value))

  def bitCount(key: String): CommandIO[Long] =
    liftF(BitCount(key, None))

  def bitCount(key: String, start: Long, end: Long): CommandIO[Long] =
    liftF(BitCount(key, Some((start, end))))

  def bitOpAnd(dest: String, sources: NonEmptyList[String]): CommandIO[Long] =
    liftF(BitOp(BitOpOperator.And(sources), dest))

  def bitOpNot(dest: String, source: String): CommandIO[Long] =
    liftF(BitOp(BitOpOperator.Not(source), dest))

  def bitOpOr(dest: String, sources: NonEmptyList[String]): CommandIO[Long] =
    liftF(BitOp(BitOpOperator.Or(sources), dest))

  def bitOpXor(dest: String, sources: NonEmptyList[String]): CommandIO[Long] =
    liftF(BitOp(BitOpOperator.Xor(sources), dest))

  def bitPos(key: String, bit: Boolean): CommandIO[Long] =
    liftF(BitPos(key, bit, None, None))

  def bitPos(key: String, bit: Boolean, start: Long): CommandIO[Long] =
    liftF(BitPos(key, bit, Option(start), None))

  def bitPos(key: String, bit: Boolean, start: Long, end: Long): CommandIO[Long] =
    liftF(BitPos(key, bit, Option(start), Option(end)))

  def decr(key: String): CommandIO[Long] =
    liftF(Decr(key))

  def decrBy(key: String, amount: Long): CommandIO[Long] =
    liftF(DecrBy(key, amount))

  def get(key: String): CommandIO[Option[String]] =
    liftF(Get(key))

  def getBit(key: String, offset: Long): CommandIO[Long] =
    liftF(GetBit(key, offset))

  def getRange(key: String, start: Long, end: Long): CommandIO[String] =
    liftF(GetRange(key, start, end))

  def getSet(key: String, value: String): CommandIO[String] =
    liftF(GetSet(key, value))

  def incr(key: String): CommandIO[Long] =
    liftF(Incr(key))

  def incrBy(key: String, amount: Long): CommandIO[Long] =
    liftF(IncrBy(key, amount))

  def incrByFloat(key: String, amount: Double): CommandIO[Long] =
    liftF(IncrByFloat(key, amount))

  def mSet(key: String, value: String): CommandIO[Unit] =
    liftF(MSet(NonEmptyList.of((key, value))))

  def mSet(key: String, value: String, multi: (String, String)*): CommandIO[Unit] =
    liftF(MSet(NonEmptyList((key, value), multi.toList)))

  // TODO Evaluate a mSet which accepts a Map as input
  // will needs to return a Validated[CommandIO[Unit]] as the map could be empty
  //  def mSet(key: String, value: String): CommandIO[Unit] =
  //    Free.liftF(MSet(NonEmptyList.of((key, value))))

  def mSetNx(key: String, value: String): CommandIO[Boolean] =
    liftF(MSetNx(NonEmptyList.of((key, value))))

  def mSetNx(key: String, value: String, multi: (String, String)*): CommandIO[Boolean] =
    liftF(MSetNx(NonEmptyList((key, value), multi.toList)))

  def set(key: String, value: String): CommandIO[Boolean] =
    liftF(Set(key, value, None, None))

  def set(key: String, value: String, expire: FiniteDuration): CommandIO[Boolean] =
    liftF(Set(key, value, Option(expire), None))

  def set(key: String, value: String, exists: Boolean): CommandIO[Boolean] =
    liftF(Set(key, value, None, Option(exists)))

  def set(key: String, value: String, expire: FiniteDuration, exists: Boolean): CommandIO[Boolean] =
    liftF(Set(key, value, Option(expire), Option(exists)))

  def setRange(key: String, offset: Long, value: String): CommandIO[Long] =
    liftF(SetRange(key, offset, value))

  def set(key: String): CommandIO[Long] =
    liftF(StrLen(key))

}
