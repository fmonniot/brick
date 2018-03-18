package eu.monniot.fs2.redis.free

import cats.data.NonEmptyList

import scala.concurrent.duration.FiniteDuration

// Algebra of operation for redis commands (except streams)
trait Commands {

  sealed trait CommandOp[A]

  /*
   * KEY Commands
   */

  //  DEL
  case class Del(keys: NonEmptyList[String]) extends CommandOp[Long]

  //  DUMP

  //  EXISTS
  case class Exists(keys: NonEmptyList[String]) extends CommandOp[Long]

  //  EXPIRE
  //  EXPIREAT
  //  KEYS
  //  MIGRATE
  //  MOVE
  //  OBJECT
  //  PERSIST
  //  PEXPIRE
  //  PEXPIREAT
  //  PTTL
  //  RANDOMKEY
  //  RENAME
  //  RENAMENX
  //  RESTORE
  //  SCAN
  //  SORT
  //  TOUCH
  //  TTL
  //  TYPE
  //  UNLINK
  //  WAIT


  /*
   * STRING Commands
   */

  case class Append(key: String, value: String) extends CommandOp[Long]

  case class BitCount(key: String, startEnd: Option[(Long, Long)]) extends CommandOp[Long]

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

  case class BitOp(op: BitOpOperator, dest: String) extends CommandOp[Long]

  case class BitPos(key: String, bit: Boolean, start: Option[Long], end: Option[Long]) extends CommandOp[Long]

  case class Decr(key: String) extends CommandOp[Long]

  case class DecrBy(key: String, amount: Long) extends CommandOp[Long]

  case class Get(key: String) extends CommandOp[Option[String]]

  case class GetBit(key: String, offset: Long) extends CommandOp[Long]

  case class GetRange(key: String, start: Long, end: Long) extends CommandOp[String]

  case class GetSet(key: String, value: String) extends CommandOp[String]

  case class Incr(key: String) extends CommandOp[Long]

  case class IncrBy(key: String, amount: Long) extends CommandOp[Long]

  case class IncrByFloat(key: String, amount: Double) extends CommandOp[Long]

  case class MGet(keys: NonEmptyList[String]) extends CommandOp[Map[String, String]]

  case class MSet(keyAndValues: NonEmptyList[(String, String)]) extends CommandOp[Unit]

  case class MSetNx(keyAndValues: NonEmptyList[(String, String)]) extends CommandOp[Boolean]

  // PSETEX
  // As indicated in Redis documentation, this can be done via the
  // SET command, as such we aren't implementing this command

  // The exists parameter is used to set the NX/XX argument (false -> NX, true -> XX)
  case class Set(key: String, value: String, ex: Option[FiniteDuration], exists: Option[Boolean]) extends CommandOp[Boolean]

  case class SetBit(key: String, offset: Long, value: Int) extends CommandOp[Long]

  // SETEX
  // As indicated in Redis documentation, this can be done via the
  // SET command, as such we aren't implementing this command

  // SETNX
  // As indicated in Redis documentation, this can be done via the
  // SET command, as such we aren't implementing this command

  case class SetRange(key: String, offset: Long, value: String) extends CommandOp[Long]

  case class StrLen(key: String) extends CommandOp[Long]

}
