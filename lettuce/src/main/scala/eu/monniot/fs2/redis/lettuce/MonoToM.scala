package eu.monniot.fs2.redis.lettuce

import java.util.function.Consumer

import cats.effect.Async
import reactor.core.publisher.Mono

// type class with implementation
// Instead we should have an instance with take an Async and return a MonoToM
trait MonoToM[F[_]] extends Async[F] {
  def fromMono[A](mono: Mono[A]): F[A] = async { cb =>
    mono.subscribe(
      (a: A) => cb(Right(a)),
      new Consumer[Throwable] {
        override def accept(t: Throwable): Unit = cb(Left(t))
      })
  }
}
