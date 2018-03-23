package eu.monniot.brick.testkit

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.ActorRef
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import cats.data.Kleisli
import cats.effect.Async
import cats.~>
import eu.monniot.brick.free.commands._
import eu.monniot.brick.free.interpreters.KleisliInterpreter

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{higherKinds, implicitConversions}


trait TestKitInterpreter[M[_]] extends KleisliInterpreter[M, ActorRef[RedisActorAlg.Command]] {

  implicit val M: Async[M]
  implicit val ec: ExecutionContext
  implicit val scheduler: Scheduler

  private implicit val timeout: Timeout = Timeout(FiniteDuration(1, "min"))

  type Connection = ActorRef[RedisActorAlg.Command]

  lazy val CommandInterpreter: CommandOp ~> Kleisli[M, Connection, ?] = new Interpreter {}

  private implicit def futureToAsync[T](future: Future[T]): M[T] =
    M.async { cb =>
      import scala.util.{Failure, Success}

      future.onComplete {
        case Success(value) => cb(Right(value))
        case Failure(throwable) => cb(Left(throwable))
      }
    }

  trait Interpreter extends (CommandOp ~> Kleisli[M, Connection, ?]) {

    private def kleisli[A](f: Connection => Future[A]) =
      Kleisli((connection: Connection) => futureToAsync(f(connection)))

    def apply[A](fa: CommandOp[A]): Kleisli[M, Connection, A] = {
      fa match {
        // KEYS
        case Del(keys) => ???

        case Exists(keys) => ???

        // STRINGS
        case Append(key, value) => ???

        case BitCount(key, None) => ???

        case BitCount(key, Some((start, end))) => ???

        case BitOp(op, dest) => kleisli(c => c ? (RedisActorAlg.BitOp(op, dest, _)))

        case BitPos(key, bit, Some(start), Some(end)) => ???

        case BitPos(key, bit, Some(start), None) => ???

        case BitPos(key, bit, _, _) => ???

        case Decr(key) => ???

        case DecrBy(key, amount) => ???

        case Get(key) => kleisli(c => c ? (RedisActorAlg.Get(key, _)))

        case GetBit(key, offset) => ???

        case GetRange(key, start, end) => ???

        case GetSet(key, value) => ???

        case Incr(key) => ???

        case IncrBy(key, amount) => ???

        case IncrByFloat(key, amount) => ???

        case MGet(keys) => ???

        case MSet(kv) => ???

        case MSetNx(kv) => ???

        case Set(key, value, expire, exists) => ???

        case SetBit(key, offset, value) => ???

        case SetRange(key, offset, value) => ???

        case StrLen(key) => ???

      }
    }
  }

}


object TestKitInterpreter {
  def apply[M[_]](implicit ev: Async[M], sys: ActorSystem): TestKitInterpreter[M] =
    new TestKitInterpreter[M] {
      val M = ev
      val ec = sys.dispatcher
      val scheduler = sys.scheduler
    }
}