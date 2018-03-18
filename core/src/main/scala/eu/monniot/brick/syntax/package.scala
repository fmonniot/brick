package eu.monniot.brick

import cats.effect.IO
import eu.monniot.brick.free.commands.CommandIO
import eu.monniot.brick.free.interpreters.Transactor


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
