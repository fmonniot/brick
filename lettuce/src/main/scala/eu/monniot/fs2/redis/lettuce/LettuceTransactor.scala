package eu.monniot.fs2.redis.lettuce

import cats.effect.{Async, Sync}
import eu.monniot.fs2.redis.free.interpreters.Transactor
import io.lettuce.core.RedisClient

object LettuceTransactor {



  object fromRedisClient {

    private def create[M[_]: Async](client: => RedisClient): Transactor.Aux[M, Unit] =
      Transactor((), _ => Sync[M].delay { client.connect.async }, LettuceInterpreter[M].CommandInterpreter)

    // url = "redis://localhost"
    def apply[M[_]: Async](url: String): Transactor.Aux[M, Unit] =
      create(RedisClient.create(url))

    def apply[M[_]: Async](client: => RedisClient): Transactor.Aux[M, Unit] =
      create(client)

  }

}
