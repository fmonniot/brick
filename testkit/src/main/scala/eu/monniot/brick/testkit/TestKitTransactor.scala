package eu.monniot.brick.testkit

import akka.actor.ActorSystem
import akka.actor.typed.scaladsl.adapter._
import cats.effect.{Async, Sync}
import eu.monniot.brick.free.interpreters.Transactor


object TestKitTransactor {

  def create[M[_] : Async](implicit system: ActorSystem): Transactor.Aux[M, Unit] = {
    val supervisor = system.spawn(RedisActor.behavior, "redis")

    Transactor((), (_) => Sync[M].delay(supervisor), TestKitInterpreter[M].CommandInterpreter)
  }
}
