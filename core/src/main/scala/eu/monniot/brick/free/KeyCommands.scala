package eu.monniot.brick.free

import cats.data.NonEmptyList
import cats.free.Free.liftF
import eu.monniot.brick.free.commands._


trait KeyCommands {

  // Smart constructors

  def del(keys: NonEmptyList[String]): CommandIO[Long] =
    liftF(Del(keys))

  def del(key: String): CommandIO[Long] =
    del(NonEmptyList.one(key))

  def del(key: String, more: String*): CommandIO[Long] =
    del(NonEmptyList(key, more.toList))

  def exists(keys: NonEmptyList[String]): CommandIO[Long] =
    liftF(Exists(keys))

  def exists(key: String): CommandIO[Long] =
    exists(NonEmptyList.one(key))

  def exists(key: String, more: String*): CommandIO[Long] =
    exists(NonEmptyList(key, more.toList))

}
