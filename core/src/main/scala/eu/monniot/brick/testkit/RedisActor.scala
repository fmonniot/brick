package eu.monniot.brick.testkit

import akka.actor.typed._
import akka.actor.typed.scaladsl.Actor
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.receptionist.ServiceKey


object RedisActor {

  val RedisServiceKey = ServiceKey[Command]("redisService")

  // This should be generated automatically and match what is in the CommandsAls file
  // We should also generate the interpreter. Which basically make a translation
  // between the free monad and the redis algebra.
  sealed trait Command

  final case class Get(key: String, replyTo: ActorRef[Option[String]]) extends Command


  // Real implementation of redis

  private def redis(db: TypedMap[String]): Behavior[Command] =
    Actor.immutable { (ctx, msg) =>
      msg match {
        case Get(key, replyTo) =>
          println(s"${ctx.self} got message $msg from $replyTo")
          replyTo ! db.get[String](key)
          Actor.same

        case m =>
          println(s"${ctx.self} got message $m")
          Actor.same
      }
    }

  val behavior: Behavior[Command] = redis(new TypedMap[String](Map.empty))
}
