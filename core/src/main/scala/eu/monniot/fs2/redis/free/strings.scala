package eu.monniot.fs2.redis.free

import cats.InjectK
import cats.data.NonEmptyList
import cats.free.Free
import eu.monniot.fs2.redis.free.commands.{CommandIO, CommandOp}

import scala.concurrent.duration.FiniteDuration

object strings extends StringCommands

trait StringCommands {

  // Algebra of operation for redis string commands group
  sealed trait StringOp[A]

  // Free Monad over StringOp
  type StringIO[A] = Free[StringOp, A]

  // APPEND
  case class Append(key: String, value: String) extends StringOp[Long]

  // BITCOUNT
  case class BitCount(key: String, startEnd: Option[(Long, Long)]) extends StringOp[Long]

  // BITFIELD
  // TODO

  // BITOP
  sealed trait BitOpOperator

  object BitOpOperator {

    case class And(keys: NonEmptyList[String]) extends BitOpOperator

    case class Not(key: String) extends BitOpOperator

    case class Or(keys: NonEmptyList[String]) extends BitOpOperator

    case class Xor(keys: NonEmptyList[String]) extends BitOpOperator

  }

  case class BitOp(op: BitOpOperator, dest: String) extends StringOp[Long]

  // BITPOS
  case class BitPos(key: String, bit: Boolean, start: Option[Long], end: Option[Long]) extends StringOp[Long]

  // DECR
  case class Decr(key: String) extends StringOp[Long]

  // DECRBY
  case class DecrBy(key: String, amount: Long) extends StringOp[Long]

  // GET
  case class Get(key: String) extends StringOp[Option[String]]

  // GETBIT
  case class GetBit(key: String, offset: Long) extends StringOp[Long]

  // GETRANGE
  case class GetRange(key: String, start: Long, end: Long) extends StringOp[String]

  // GETSET
  case class GetSet(key: String, value: String) extends StringOp[String]

  // INCR
  case class Incr(key: String) extends StringOp[Long]

  // INCRBY
  case class IncrBy(key: String, amount: Long) extends StringOp[Long]

  // INCRBYFLOAT
  case class IncrByFloat(key: String, amount: Double) extends StringOp[Long]

  // MGET
  case class MGet(keys: NonEmptyList[String]) extends StringOp[Map[String, String]]

  // MSET
  case class MSet(keyAndValues: NonEmptyList[(String, String)]) extends StringOp[Unit]

  // MSETNX
  case class MSetNx(keyAndValues: NonEmptyList[(String, String)]) extends StringOp[Boolean]

  // PSETEX
  // As indicated in Redis documentation, this can be done via the
  // SET command, as such we aren't implementing this command

  // SET key value [EX seconds] [PX milliseconds] [NX|XX]
  // The exists parameter is used to set the NX/XX argument (false -> NX, true -> XX)
  case class Set(key: String, value: String, ex: Option[FiniteDuration], exists: Option[Boolean]) extends StringOp[Boolean]

  // SETBIT
  case class SetBit(key: String, offset: Long, value: Int) extends StringOp[Long]

  // SETEX
  // As indicated in Redis documentation, this can be done via the
  // SET command, as such we aren't implementing this command

  // SETNX
  // As indicated in Redis documentation, this can be done via the
  // SET command, as such we aren't implementing this command

  // SETRANGE
  case class SetRange(key: String, offset: Long, value: String) extends StringOp[Long]

  // STRLEN
  case class StrLen(key: String) extends StringOp[Long]


  // Smart constructors

  def append(key: String, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(Append(key, value))

  def bitCount(key: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitCount(key, None))

  def bitCount(key: String, start: Long, end: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitCount(key, Some((start, end))))

  def bitOpAnd(dest: String, sources: NonEmptyList[String])(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitOp(BitOpOperator.And(sources), dest))

  def bitOpNot(dest: String, source: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitOp(BitOpOperator.Not(source), dest))

  def bitOpOr(dest: String, sources: NonEmptyList[String])(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitOp(BitOpOperator.Or(sources), dest))

  def bitOpXor(dest: String, sources: NonEmptyList[String])(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitOp(BitOpOperator.Xor(sources), dest))

  def bitPos(key: String, bit: Boolean)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitPos(key, bit, None, None))

  def bitPos(key: String, bit: Boolean, start: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitPos(key, bit, Option(start), None))

  def bitPos(key: String, bit: Boolean, start: Long, end: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(BitPos(key, bit, Option(start), Option(end)))

  def decr(key: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(Decr(key))

  def decrBy(key: String, amount: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(DecrBy(key, amount))

  def get(key: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Option[String]] =
    Free.inject(Get(key))

  def getBit(key: String, offset: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(GetBit(key, offset))

  def getRange(key: String, start: Long, end: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[String] =
    Free.inject(GetRange(key, start, end))

  def getSet(key: String, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[String] =
    Free.inject(GetSet(key, value))

  def incr(key: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(Incr(key))

  def incrBy(key: String, amount: Long)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(IncrBy(key, amount))

  def incrByFloat(key: String, amount: Double)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(IncrByFloat(key, amount))

  def mSet(key: String, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Unit] =
    Free.inject(MSet(NonEmptyList.of((key, value))))

  def mSet(key: String, value: String, multi: (String, String)*)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Unit] =
    Free.inject(MSet(NonEmptyList((key, value), multi.toList)))

  // TODO Evaluate a mSet which accepts a Map as input
  // will needs to return a Validated[CommandIO[Unit]] as the map could be empty
  //  def mSet(key: String, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Unit] =
  //    Free.inject(MSet(NonEmptyList.of((key, value))))

  def mSetNx(key: String, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Boolean] =
    Free.inject(MSetNx(NonEmptyList.of((key, value))))

  def mSetNx(key: String, value: String, multi: (String, String)*)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Boolean] =
    Free.inject(MSetNx(NonEmptyList((key, value), multi.toList)))

  def set(key: String, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Boolean] =
    Free.inject(Set(key, value, None, None))

  def set(key: String, value: String, expire: FiniteDuration)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Boolean] =
    Free.inject(Set(key, value, Option(expire), None))

  def set(key: String, value: String, exists: Boolean)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Boolean] =
    Free.inject(Set(key, value, None, Option(exists)))

  def set(key: String, value: String, expire: FiniteDuration, exists: Boolean)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Boolean] =
    Free.inject(Set(key, value, Option(expire), Option(exists)))

  def setRange(key: String, offset: Long, value: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(SetRange(key, offset, value))

  def set(key: String)(implicit I: InjectK[StringOp, CommandOp]): CommandIO[Long] =
    Free.inject(StrLen(key))

}
