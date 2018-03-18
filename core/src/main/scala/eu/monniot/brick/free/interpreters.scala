package eu.monniot.brick.free

import cats.data.Kleisli
import cats.effect.Async
import cats.implicits._
import cats.{Monad, ~>}
import eu.monniot.brick.free.commands._

import scala.language.{higherKinds, reflectiveCalls}


object interpreters {

  // M is the effect monad, often IO
  // C is the connection type (for lettuce it's the Commands interfaces)
  type Interpreter[M[_], C] = CommandOp ~> Kleisli[M, C, ?]


  // find another name, as it does a bit more than just transaction
  // TODO Update the scaladoc to reflect the reality :)
  sealed abstract class Transactor[M[_]] {
    self =>

    /** An arbitrary value that will be handed back to `connect` **/
    type A

    /** An arbitrary value which represent the Redis Connection */
    type Conn

    /** An arbitrary value, meaningful to the instance **/
    def kernel: A

    /** A program in `M` that can provide a database connection, given the kernel **/
    def connect: A => M[Conn]

    /** A natural transformation for interpreting `ConnectionIO` **/
    def interpret: Interpreter[M, Conn]


    /**
      * Construct a program to peform arbitrary configuration on the kernel value (changing the
      * timeout on a connection pool, for example). This can be the basis for constructing a
      * configuration language for a specific kernel type `A`, whose operations can be added to
      * compatible `Transactor`s via implicit conversion.
      *
      * @group Configuration
      */
    def configure[B](f: A => M[B]): M[B] =
      f(kernel)

    // run the given program as is
    def exec(implicit ev: Monad[M]): CommandIO ~> M =
      λ[CommandIO ~> M](io => connect(kernel).flatMap(io.foldMap(interpret).run))

    // run the given program in a MULTI/EXEC transaction
    def multi(implicit ev: Monad[M]): CommandIO ~> M =
      λ[CommandIO ~> M] { io =>
        // TODO Compose io between a MULTI and an EXEC
        connect(kernel).flatMap(io.foldMap(interpret).run)
      }
  }

  object Transactor {

    def apply[M[_], A0, Conn0](kernel0: A0,
                               connect0: A0 => M[Conn0],
                               interpret0: Interpreter[M, Conn0]
                              ): Transactor.Aux[M, A0] = new Transactor[M] {
      type A = A0
      type Conn = Conn0
      val kernel = kernel0
      val connect = connect0
      val interpret = interpret0
    }

    type Aux[M[_], A0] = Transactor[M] {type A = A0}
  }

  trait KleisliInterpreter[M[_], Conn] {
    implicit val M: Async[M]

    def CommandInterpreter: CommandOp ~> Kleisli[M, Conn, ?]

    def connection[A](op: CommandOp[A]): Kleisli[M, Conn, A] =
      Kleisli(conn => CommandInterpreter(op).run(conn))

  }


}