package eu.monniot.fs2

import cats._
import cats.effect._
import cats.implicits._
import cats.effect.IO
import eu.monniot.fs2.redis.free.commands.CommandIO
import eu.monniot.fs2.redis.free.interpreters.Transactor
import eu.monniot.fs2.redis.free.{keys, strings}

// TODO Add to the build.sc instead, as we are going to use them basically everywhere
import scala.language.higherKinds

package object redis {

  // TODO Move that part as a test
  // Testing out the compilation

  val program: CommandIO[Option[String]] = for {
    r <- strings.get("key")
    _ <- keys.del("key")
  } yield r

  // TODO What does that do ?
  val t = 42.pure[CommandIO]

  implicit val xa: Transactor[IO] = ???


  // In a MULTI/EXEC block
  val multiResult: IO[Option[String]] = program.multi

  // As is
  val directResult: IO[Option[String]] = program.exec


  // TODO Move to syntax package
  implicit class CommandIoOps[A](val io: CommandIO[A]) extends AnyVal {
    def exec(implicit xa: Transactor[IO]) =
      xa.exec.apply(io)

    def multi(implicit xa: Transactor[IO]) =
      xa.multi.apply(io)
  }

}
