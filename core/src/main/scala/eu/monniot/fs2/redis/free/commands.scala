package eu.monniot.fs2.redis.free

import cats.data.EitherK
import cats.free.Free
import eu.monniot.fs2.redis.free.keys.KeyOp
import eu.monniot.fs2.redis.free.strings.StringOp

object commands {

  type C0[A] = EitherK[KeyOp, StringOp, A]
  type CommandOp[A] = C0[A]
  type CommandIO[A] = Free[CommandOp, A]

  // Alias to make it easily discoverable
  trait AllCommands extends KeyCommands
    with StringCommands

  // Alias to make the commands easily discoverable
  // TODO Find out why in that case the InjectK instances aren't found
  object all extends AllCommands

}
