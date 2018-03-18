package eu.monniot.fs2

import cats.effect.IO
import eu.monniot.fs2.redis.free.commands.CommandIO
import eu.monniot.fs2.redis.free.interpreters.Transactor
import eu.monniot.fs2.redis.syntax._


package object redis {

  // TODO Move that part as a test
  val program: CommandIO[Option[String]] = {
    import redis.free.commands.all._

    for {
      r <- get("key")
      _ <- del("key")
    } yield r
  }

  // brick-core doesn't provides a default Transactor, use one from the submodules
  implicit val xa: Transactor[IO] = ???

  // In a MULTI/EXEC block
  val multiResult: IO[Option[String]] = program.multi

  // As is (default mode depends on the driver, eg. lettuce default to pipeline)
  val directResult: IO[Option[String]] = program.exec

}
