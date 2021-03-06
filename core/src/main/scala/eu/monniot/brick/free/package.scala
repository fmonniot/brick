package eu.monniot.brick

import cats.free.Free


package object free {


  object commands extends CommandsAlg {

    // Free Monad over CommandOp
    type CommandIO[A] = Free[CommandOp, A]


    // Alias to make the commands easily discoverable

    object all extends AllCommands

    object keys extends KeyCommands

    object strings extends StringCommands

  }

}
