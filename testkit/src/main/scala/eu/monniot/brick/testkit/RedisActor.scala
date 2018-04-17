package eu.monniot.brick.testkit

import akka.actor.typed._
import akka.actor.typed.receptionist.ServiceKey
import akka.actor.typed.scaladsl.Actor
import cats.data.NonEmptyList
import eu.monniot.brick.free.commands.BitOpOperator
import eu.monniot.brick.testkit.RedisActorAlg._
import eu.monniot.brick.testkit.TypedMap._


object RedisActor {

  val RedisServiceKey = ServiceKey[Command]("redisService")

  val behavior: Behavior[Command] = redis(new TypedMap[String](Map.empty))

  // "Real" implementation of redis
  // Please note that we take shortcut when computing things here.
  // For example, when deleting keys we don't do anything atomically.
  // This is ok as the Actor doesn't authorize concurrent access.
  private def redis(db: TypedMap[String]): Behavior[Command] =
    Actor.immutable {
      case (_, Del(keys, replyTo)) =>
        replyTo ! countKeys(db, keys)

        redis(keys.foldLeft(db) { case (redis, key) => redis.remove(key) })

      case (_, Exists(keys, replyTo)) =>
        replyTo ! countKeys(db, keys)

        Actor.same

      case (_, Append(key, value, replyTo)) =>
        var r: Long = 0

        val ndb = db.replace[String](key) {
          case Some(s) =>
            r = s.length + value.length
            (s + value).typed
          case None =>
            r = value.length
            value.typed
        }

        replyTo ! r

        redis(ndb)

      case (_, BitCount(key, option, replyTo)) =>
        val f: Seq[Byte] => Seq[Byte] = option match {
          case Some((start, end)) => adaptSequence(start, end)(_)
          case None => identity
        }

        val count = db.get[String](key).map { s =>

          val bits = f(s.getBytes).flatMap(b => byte2Bools(b))

          bits.count(identity)
        }.getOrElse(0)

        replyTo ! count

        Actor.same


      case (_, BitOp(BitOpOperator.Not(key), dest, replyTo)) =>


        Actor.same

      case (_, BitOp(op, dest, replyTo)) =>

        val f: (Boolean, Boolean) => Boolean = op match {
          case _: BitOpOperator.And => (a, b) => a & b
          case _: BitOpOperator.Not => ???
          case _: BitOpOperator.Or => (a, b) => a || b
          case _: BitOpOperator.Xor => (a, b) => a ^ b
        }

        val keys: NonEmptyList[String] = op match {
          case BitOpOperator.And(k) => k
          case BitOpOperator.Not(k) => ???
          case BitOpOperator.Or(k) => k
          case BitOpOperator.Xor(k) => k
        }

        type Str = Seq[Boolean]

        val values: List[Str] = keys.toList.flatMap(db.get[String](_))
          .map(str => str.getBytes.toSeq.flatMap(byte2Bools))

        val maxLen = values.foldLeft(0) { case (max, str) => if (max > str.length) max else str.length }

        val sameLengthStrings: Seq[Str] = values.map { str =>
          if (str.length < maxLen) str.padTo(maxLen, false)
          else str
        }


        // Seq[Seq[T]]
        /*
        Seq(
          Seq("a", "b", "c"),
          Seq("A", "B", "C"),
          Seq("1", "2", "3")
        )
         */
        val destBools = for {
          index <- 1 to maxLen
          column = sameLengthStrings.map(_(index))
        } yield {
          column.size match {
            case 1 =>
              column.head
            case 2 =>
              f(column.head, column.last)
            case _ =>
              val z = f(column.head, column.tail.head)

              column.tail.tail.foldLeft(z) { case (acc, b) => f(acc, b)}
          }
        }

        val destString = destBools.grouped(8).map(_.foldLeft(0)((i, b) => (i << 1) + (if(b) 1 else 0)).toChar).mkString


        Actor.same

      case (_, Get(key, replyTo)) =>
        replyTo ! db.get[String](key)

        Actor.same

      // TODO Real implementation
      case (_, Set(key, value, ex, exists, replyTo)) =>
        replyTo ! true
        redis(db.replace[String](key) {
           _ => value.typed
        })

      // This can be commented to verify that all messages have been implemented
      // In fact, post development this case won't exists anymore :)
      case (ctx, msg) =>
        println(s"${ctx.self} got message $msg but don't know how to handle it (yet). Discarding the message.")
        Actor.same
    }

  private def countKeys(db: TypedMap[String], keys: NonEmptyList[String]): Long =
    keys.foldLeft(0) { case (count, k) =>
      if (db.haveKey(k)) count + 1
      else count
    }

  private def isBitSet(byte: Byte)(bit: Int): Boolean =
    ((byte >> bit) & 1) == 1

  private def byte2Bools(b: Byte): Seq[Boolean] =
    0 to 7 map isBitSet(b)

  // if start and end are out of range, limit them to the actual sequence length
  private def adaptSequence(start: Long, end: Long)(seq: Seq[Byte]): Seq[Byte] = {
    val size = seq.size

    val realStart = if (start < 0) size + start else start
    val realEnd = if (end < 0) size + end else end

    seq.slice(realStart.toInt, realEnd.toInt + 1)
  }

}
