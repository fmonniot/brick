package eu.monniot.brick.testkit

import akka.actor.ActorSystem
import cats.effect.IO
import eu.monniot.brick
import eu.monniot.brick.free.commands.CommandIO
import eu.monniot.brick.syntax._
import eu.monniot.brick.free.interpreters.Transactor
import org.scalatest.FlatSpec

class Testing extends FlatSpec {

  implicit val system = ActorSystem("tests")

  it should "do something" in {

    val program = {
      import brick.free.commands.all._

      for {
        r <- set("key", "value")
        v <- get("key")
      } yield (r, v)
    }

    implicit val xa: Transactor[IO] = TestKitTransactor.create[IO]

    val directResult = program.exec.unsafeRunSync()

    println(directResult)
  }
}
