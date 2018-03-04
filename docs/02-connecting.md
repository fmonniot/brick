# 02 Connecting to a Database

In this chapter we start from the beginning. First we write a program that connects to a Redis database and returns a value, and then we run that program in the REPL. We also touch a composing small programs to construct larger ones.

## Our First program

Before we can use **brick** we need to import some symbols. We will use package imports here as a convenience; this will give us the most commonly-used symbols when working with the API.

```scala
import eu.monniot.brick._
```

Let's also bring in Cats.

```scala
import cats._
import cats.effect._
import cats.implicits._
```

In the **brick** API the most common types we will deal with have the form `CommandIO[A]`, specifying computations that take place in a context where a Redis session is available, ultimately producing a value of type `A`.

So let's start a `CommandIO[A]` program that simply returns a constant.

```scala
scala> val program1 = 42.pure[CommandIO]
program1: eu.monniot.brick.CommandIO[Int] = Free(...)
```

This is a perfectly respectable **brick** program, but we can't run it as-is; we need a _Redis session_ first. There are several ways to do this, but here let's use a `Transactor`.

```scala
// A transactor that gets connections from io.lettuce.core.RedisClient
val xa = lettuce.Transactor.fromRedisClient("redis://password@localhost:6379/0")
```

A `Transactor` is a data type that knows how to connect to a Redis instance; and with this knowledge it can transform `CommandIO ~> IO`, which gives us a program we can run. Specifically it gives us an `IO` that, when run, will connect to the database and execute a single transaction.

We are using `cats.effect.IO` as our final effect type, but you can use any monad `M[_]` given `cats.effect.Async[M]`. See [_Using Your Own Target Monad_] at the end of this chapter for more details.

The `Transactor` we used simply delegates to the `io.lettuce.core.StatefulRedisConnection<K, V>` to allocate and manage connections.

And here we go.

```repl
scala> val io = program1.exec(xa)
io: cats.effect.IO[Int] = IO$1853102201

scala> io.unsafeRunSync()
res1: Int = 42
```

Hooray! We have computed a constant. It's not very interesting because we never ask the database to perform any work, but it's a first step.

> Keep in mind that all the code in this documentation is pure _except_ the calls to `IO.unsafeRunSync`, which is the "end of the world" operation that typically appears only at your application's entry points. In the REPL we use it to force a computation to "happen".

Right. Now let's try something more interesting.

## Our Second Program

Now let's use the `echo` commands to construct a query that asks _Redis_ to return a constant.


```repl
scala> val program2 = echo("42")
program2: eu.monniot.brick.CommandIO[String] = Free(...)

scala> val io2 = program2.exec(xa)
io2: cats.effect.IO[String]

scala> io2.unsafeRunSync()
res2: String = 42
```

Ok! We have now connected to a Redis instance to compute a constant. Considerably more impressive.

## Our Third Program

What if we want to do more than one thing? Easy! `CommandIO` is a monad, so we can use a `for` comprehension to compose two smaller programs into one larger program.

```scala
val program3a: CommandIO[String] = for {
    a <- get("myCompositeKey")
    b = a.getOrElse("a:b:c").split(":").mkString(";")
    c <- set("myCompositeKey", b)
} yield c
```

And Io, it was good:

```repl
scala> program3a.exec(xa).unsafeRunSync
res4: String = a;b;c
```

## Diving Deeper

All of the **brick** monads are implemented via `cats.free.Free` and have no operational semantics; we can only "run" a **brick** program by transforming `CommandIO` to a monad that actually has some meaning.

Out of the box **brick** doesn't provides any interpreter, as they need to rely on an actual Redis Connection. It does provides the skeleton to build an interpreter from its free monads to `Kleisli[M, CommandIO, ?]` given `Async[M]`.

```scala
scala> import cats.~>
import cats.$tilde$greater

scala> import cats.data.Kleisli
import cats.data.Kleisli

scala> import eu.monniot.brick.free.command.CommandOp
import eu.monniot.brick.free.command.CommandOp

scala> val interpreter = new interpreters.KleisliInterpreter[IO] {
    val KeysInterpreter = null
    val StringsInterpreter = null
    ...
}
// TODO Return type

scala> val kleisli = program1.foldMap(interpreter)

scala> val io = IO(null: ???) >>= kleisli.run
io: cats.effect.IO[Int] = IO$2938495283

scala> io.unsafeRunSync() // sneaky; program1 never looks at the connection
res6: Int = 42
```

So the interpreter above is used to transform a `CommandIO[A]` program into a `Kleisli[IO, Conn, A]`. Then we construct an `IO[Conn]` (returning `null`) and bind it through the `Kleisli`, yielding our `IO[Int]`. This of course only works because `program1` is a pure value that does not look at the connection.

The `Transactor` that we defined at the beginning of this chapter is basically a utility that allows us to do the same as above using `program1.exec(xa)`.

## Using Your Own Target Monad

As mentionned earlier, you can any monad `M[_]` given `cats.effect.Async[M]`. For example, here we use [_Monix_] `Task`.

```scala
import monix.eval.Task

val mxa = Transactor.fromRedisClient[Task]("redis://localhost:6379/0")

val task: Task[String] = get("key").exec
```
