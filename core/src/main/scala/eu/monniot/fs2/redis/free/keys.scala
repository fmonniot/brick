package eu.monniot.fs2.redis.free

import cats.InjectK
import cats.data.NonEmptyList
import cats.free.Free
import eu.monniot.fs2.redis.free.commands.{CommandOp, CommandIO}

object keys extends KeyCommands

trait KeyCommands {

  // Algebra of operation for redis key commands group
  sealed trait KeyOp[A]

  // Free Monad over KeyOp
  type KeyIO[A] = Free[KeyOp, A]

  // Alias for the injection
  type Inj = KeyOp InjectK CommandOp

  //  DEL
  case class Del(keys: NonEmptyList[String]) extends KeyOp[Long]

  //  DUMP
  //  EXISTS
  case class Exists(keys: NonEmptyList[String]) extends KeyOp[Long]

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


  // Smart constructors

  def del(keys: NonEmptyList[String])(implicit I: Inj): CommandIO[Long] =
    Free.inject(Del(keys))

  def del(key: String)(implicit I: Inj): CommandIO[Long] =
    del(NonEmptyList.one(key))

  def del(key: String, more: String*)(implicit I: Inj): CommandIO[Long] =
    del(NonEmptyList(key, more.toList))

  def exists(keys: NonEmptyList[String])(implicit I: Inj): CommandIO[Long] =
    Free.inject(Exists(keys))

  def exists(key: String)(implicit I: Inj): CommandIO[Long] =
    exists(NonEmptyList.one(key))

  def exists(key: String, more: String*)(implicit I: Inj): CommandIO[Long] =
    exists(NonEmptyList(key, more.toList))

}
