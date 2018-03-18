package eu.monniot.fs2.redis

import cats.free.Free


package object free {


  object commands extends Commands {

    // Free Monad over CommandOp
    type CommandIO[A] = Free[CommandOp, A]


    // Alias to make the commands easily discoverable

    object all extends AllCommands

    object keys extends KeyCommands

    object strings extends StringCommands

  }

}
