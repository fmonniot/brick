package eu.monniot.fs2.redis

import cats.effect.IO
import eu.monniot.fs2.redis.free.commands.CommandIO
import eu.monniot.fs2.redis.free.interpreters.Transactor


// TODO Follow Cats packaging and put everything into an all package
// And then group per feature set
package object syntax {


  implicit class CommandIoOps[A](val io: CommandIO[A]) extends AnyVal {
    def exec(implicit xa: Transactor[IO]) =
      xa.exec.apply(io)

    def multi(implicit xa: Transactor[IO]) =
      xa.multi.apply(io)
  }
}
