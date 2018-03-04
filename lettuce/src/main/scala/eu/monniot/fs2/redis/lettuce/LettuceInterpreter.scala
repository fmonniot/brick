package eu.monniot.fs2.redis.lettuce

import cats.data.Kleisli
import cats.~>
import eu.monniot.fs2.redis.free.interpreters.KleisliInterpreter
import eu.monniot.fs2.redis.free.keys.KeyOp
import eu.monniot.fs2.redis.free.strings.StringOp
import io.lettuce.core.{KeyValue, SetArgs}
import io.lettuce.core.api.reactive.RedisReactiveCommands
import org.reactivestreams.{Subscriber, Subscription}
import reactor.core.publisher.Mono

import scala.collection.JavaConverters._
import scala.language.higherKinds


trait LettuceInterpreter[M[_]] extends KleisliInterpreter[M, RedisReactiveCommands[String, String]] {
  implicit val M: MonoToM[M] // Async + conversion from Mono

  type Connection = RedisReactiveCommands[String, String]

  lazy val keysInterpreter: KeyOp ~> Kleisli[M, Connection, ?] = new KeysInterpreter {}
  lazy val stringsInterpreter: StringOp ~> Kleisli[M, Connection, ?] = new StringsInterpreter {}


  trait KeysInterpreter extends (KeyOp ~> Kleisli[M, Connection, ?]) {

    import eu.monniot.fs2.redis.free.keys._

    private def withMono[A](f: Connection => Mono[A]) =
      Kleisli((conn: Connection) => M.fromMono(f(conn)))

    def apply[A](fa: KeyOp[A]) = fa match {
      case Del(keys) => withMono(_.del(keys.toList: _*)).map(_.toLong)
      case Exists(keys) => withMono(_.exists(keys.toList: _*)).map(_.toLong)
    }
  }

  trait StringsInterpreter extends (StringOp ~> Kleisli[M, Connection, ?]) {

    import eu.monniot.fs2.redis.free.strings._

    private def withMono[A](f: Connection => Mono[A]) =
      Kleisli((conn: Connection) => M.fromMono(f(conn)))

    def apply[A](fa: StringOp[A]) = fa match {
      case Append(key, value) => withMono(_.append(key, value)).map(_.toLong)
      case BitCount(key, None) => withMono(_.bitcount(key)).map(_.toLong)
      case BitCount(key, Some((start, end))) =>
        withMono(_.bitcount(key, start, end)).map(_.toLong)
      case BitOp(BitOpOperator.And(keys), dest) =>
        withMono(_.bitopAnd(dest, keys.toList: _*)).map(_.toLong)
      case BitOp(BitOpOperator.Not(src), dest) =>
        withMono(_.bitopNot(dest, src)).map(_.toLong)
      case BitOp(BitOpOperator.Or(keys), dest) =>
        withMono(_.bitopOr(dest, keys.toList: _*)).map(_.toLong)
      case BitOp(BitOpOperator.Xor(keys), dest) =>
        withMono(_.bitopXor(dest, keys.toList: _*)).map(_.toLong)
      case BitPos(key, bit, Some(start), Some(end)) =>
        withMono(_.bitpos(key, bit, start, end)).map(_.toLong)
      case BitPos(key, bit, Some(start), None) =>
        withMono(_.bitpos(key, bit, start)).map(_.toLong)
      case BitPos(key, bit, _, _) =>
        withMono(_.bitpos(key, bit)).map(_.toLong)
      case Decr(key) => withMono(_.decr(key)).map(_.toLong)
      case DecrBy(key, amount) => withMono(_.decrby(key, amount)).map(_.toLong)
      case Get(key) => withMono(_.get(key)).map(Option(_))
      case GetBit(key, offset) => withMono(_.getbit(key, offset)).map(_.toLong)
      case GetRange(key, start, end) => withMono(_.getrange(key, start, end))
      case GetSet(key, value) => withMono(_.getset(key, value))
      case Incr(key) => withMono(_.incr(key)).map(_.toLong)
      case IncrBy(key, amount) => withMono(_.incrby(key, amount)).map(_.toLong)
      case IncrByFloat(key, amount) => withMono(_.incrbyfloat(key, amount)).map(_.toLong)
      case MGet(keys) => Kleisli { conn: Connection =>

        M.async { cb =>
          conn.mget(keys.toList: _*).subscribe(new Subscriber[KeyValue[String, String]] {

            val m = scala.collection.mutable.Map.empty[String, String]

            override def onError(t: Throwable): Unit = cb(Left(t))

            override def onComplete(): Unit = cb(Right(m.toMap))

            override def onNext(t: KeyValue[String, String]): Unit =
              m + (t.getKey -> t.getValue)

            override def onSubscribe(s: Subscription): Unit =
              s.request(Long.MaxValue)
          })
        }
      }

      case MSet(kv) => withMono(_.mset(kv.toList.toMap.asJava)).map(_ => ())
      case MSetNx(kv) => withMono(_.msetnx(kv.toList.toMap.asJava)).map(b => b)
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

        withMono(_.set(key, value, args)).map(_ == "OK")
      case SetBit(key, offset, value) =>
        withMono(_.setbit(key, offset, value)).map(l => l)
      case SetRange(key, offset, value) =>
        withMono(_.setrange(key, offset, value)).map(l => l)
      case StrLen(key) =>
        withMono(_.strlen(key)).map(l => l)
    }
  }

}


object LettuceInterpreter {
  def apply[M[_]](implicit ev: MonoToM[M]): LettuceInterpreter[M] =
    new LettuceInterpreter[M] {
      val M = ev
    }
}