package eu.monniot.fs2.redis.lettuce

import java.util.concurrent.CompletionStage
import java.util.function.BiConsumer

import cats.data.Kleisli
import cats.effect.Async
import cats.~>
import eu.monniot.fs2.redis.free.interpreters.KleisliInterpreter
import eu.monniot.fs2.redis.free.keys.KeyOp
import eu.monniot.fs2.redis.free.strings.StringOp
import io.lettuce.core.SetArgs
import io.lettuce.core.api.async.RedisAsyncCommands

import scala.collection.JavaConverters._
import scala.language.{higherKinds, implicitConversions}


trait LettuceInterpreter[M[_]] extends KleisliInterpreter[M, RedisAsyncCommands[String, String]] {
  implicit val M: Async[M]

  type Connection = RedisAsyncCommands[String, String]

  lazy val keysInterpreter: KeyOp ~> Kleisli[M, Connection, ?] = new KeysInterpreter {}
  lazy val stringsInterpreter: StringOp ~> Kleisli[M, Connection, ?] = new StringsInterpreter {}

  private implicit def completionStageToAsync[T](stage: CompletionStage[T]): M[T] =
    M.async { cb =>
      stage.whenComplete(new BiConsumer[T, Throwable] {
        override def accept(t: T, u: Throwable) = {
          if(u != null) cb(Left(u))
          else cb(Right(t))
        }
      })
    }

  private def kleisli[A](f: Connection => M[A]) = Kleisli((conn: Connection) => f(conn))

  trait KeysInterpreter extends (KeyOp ~> Kleisli[M, Connection, ?]) {

    import eu.monniot.fs2.redis.free.keys._

    def apply[A](fa: KeyOp[A]) = fa match {
      case Del(keys) => kleisli(_.del(keys.toList: _*)).map(_.toLong)
      case Exists(keys) => kleisli(_.exists(keys.toList: _*)).map(_.toLong)
    }
  }

  trait StringsInterpreter extends (StringOp ~> Kleisli[M, Connection, ?]) {

    import eu.monniot.fs2.redis.free.strings._

    def apply[A](fa: StringOp[A]) = fa match {
      case Append(key, value) => kleisli(_.append(key, value)).map(_.toLong)
      case BitCount(key, None) => kleisli(_.bitcount(key)).map(_.toLong)
      case BitCount(key, Some((start, end))) =>
        kleisli(_.bitcount(key, start, end)).map(_.toLong)
      case BitOp(BitOpOperator.And(keys), dest) =>
        kleisli(_.bitopAnd(dest, keys.toList: _*)).map(_.toLong)
      case BitOp(BitOpOperator.Not(src), dest) =>
        kleisli(_.bitopNot(dest, src)).map(_.toLong)
      case BitOp(BitOpOperator.Or(keys), dest) =>
        kleisli(_.bitopOr(dest, keys.toList: _*)).map(_.toLong)
      case BitOp(BitOpOperator.Xor(keys), dest) =>
        kleisli(_.bitopXor(dest, keys.toList: _*)).map(_.toLong)
      case BitPos(key, bit, Some(start), Some(end)) =>
        kleisli(_.bitpos(key, bit, start, end)).map(_.toLong)
      case BitPos(key, bit, Some(start), None) =>
        kleisli(_.bitpos(key, bit, start)).map(_.toLong)
      case BitPos(key, bit, _, _) =>
        kleisli(_.bitpos(key, bit)).map(_.toLong)
      case Decr(key) => kleisli(_.decr(key)).map(_.toLong)
      case DecrBy(key, amount) => kleisli(_.decrby(key, amount)).map(_.toLong)
      case Get(key) => kleisli(_.get(key)).map(Option(_))
      case GetBit(key, offset) => kleisli(_.getbit(key, offset)).map(_.toLong)
      case GetRange(key, start, end) => kleisli(_.getrange(key, start, end))
      case GetSet(key, value) => kleisli(_.getset(key, value))
      case Incr(key) => kleisli(_.incr(key)).map(_.toLong)
      case IncrBy(key, amount) => kleisli(_.incrby(key, amount)).map(_.toLong)
      case IncrByFloat(key, amount) => kleisli(_.incrbyfloat(key, amount)).map(_.toLong)
      case MGet(keys) =>
        kleisli(_.mget(keys.toList: _*)).map { list =>
          list.asScala.map(kv => (kv.getKey, kv.getValue)).toMap
        }

      case MSet(kv) => kleisli(_.mset(kv.toList.toMap.asJava)).map(_ => ())
      case MSetNx(kv) => kleisli(_.msetnx(kv.toList.toMap.asJava)).map(b => b)
      case Set(key, value, expire, exists) =>
        val seconds = expire.map(_.toSeconds)
        val ms = expire.map(_.toMillis).map(_ % 1000)

        val args = new SetArgs

        seconds.foreach(args.ex)
        ms.foreach(args.px)
        exists.foreach {
          case true => args.xx()
          case false => args.nx()
        }

        kleisli(_.set(key, value, args)).map(_ == "OK")
      case SetBit(key, offset, value) =>
        kleisli(_.setbit(key, offset, value)).map(l => l)
      case SetRange(key, offset, value) =>
        kleisli(_.setrange(key, offset, value)).map(l => l)
      case StrLen(key) =>
        kleisli(_.strlen(key)).map(l => l)
    }
  }

}


object LettuceInterpreter {
  def apply[M[_]](implicit ev: Async[M]): LettuceInterpreter[M] =
    new LettuceInterpreter[M] {
      val M = ev
    }
}