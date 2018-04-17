package eu.monniot.brick.testkit

import akka.actor.typed._
import cats.data.NonEmptyList
import eu.monniot.brick.free.commands.BitOpOperator

import scala.concurrent.duration.FiniteDuration

object RedisActorAlg {

  sealed trait Command

  case class Del(keys: NonEmptyList[String], replyTo: ActorRef[Long]) extends Command

  case class Exists(keys: NonEmptyList[String], replyTo: ActorRef[Long]) extends Command

  case class Append(key: String, value: String, replyTo: ActorRef[Long]) extends Command

  case class BitCount(key: String, startEnd: Option[(Long, Long)], replyTo: ActorRef[Long]) extends Command

  case class BitOp(op: BitOpOperator, dest: String, replyTo: ActorRef[Long]) extends Command

  case class BitPos(key: String, bit: Boolean, start: Option[Long], end: Option[Long], replyTo: ActorRef[Long]) extends Command

  case class Decr(key: String, replyTo: ActorRef[Long]) extends Command

  case class DecrBy(key: String, amount: Long, replyTo: ActorRef[Long]) extends Command

  case class Get(key: String, replyTo: ActorRef[Option[String]]) extends Command

  case class GetBit(key: String, offset: Long, replyTo: ActorRef[Long]) extends Command

  case class GetRange(key: String, start: Long, end: Long, replyTo: ActorRef[String]) extends Command

  case class GetSet(key: String, value: String, replyTo: ActorRef[String]) extends Command

  case class Incr(key: String, replyTo: ActorRef[Long]) extends Command

  case class IncrBy(key: String, amount: Long, replyTo: ActorRef[Long]) extends Command

  case class IncrByFloat(key: String, amount: Double, replyTo: ActorRef[Long]) extends Command

  case class MGet(keys: NonEmptyList[String], replyTo: ActorRef[Map[String, String]]) extends Command

  case class MSet(keyAndValues: NonEmptyList[(String, String)], replyTo: ActorRef[Unit]) extends Command

  case class MSetNx(keyAndValues: NonEmptyList[(String, String)], replyTo: ActorRef[Boolean]) extends Command

  case class Set(key: String, value: String, ex: Option[FiniteDuration], exists: Option[Boolean], replyTo: ActorRef[Boolean]) extends Command

  case class SetBit(key: String, offset: Long, value: Int, replyTo: ActorRef[Long]) extends Command

  case class SetRange(key: String, offset: Long, value: String, replyTo: ActorRef[Long]) extends Command

  case class StrLen(key: String, replyTo: ActorRef[Long]) extends Command

}