# 01 Introduction

This is a very short documentation about **brick**, a pure-functional Redis layer for Scala.

This documentation is organized in a cookbook-style: we demonstrate a common task and then explain how it works. The goal is to get you up and running quickly, but give you a handle on the deeper stuff if you need it later on.

## Target Audience

This library is designed for people who are interested in typed, pure functional programming. If you are not a [Cats] user or are not familiar with functional I/O and monadic effects, you may need to go slowly and may want to spend some time reading [Functional Programming in Scala], which introduces all of the ideas that you will find when exploring **brick**.

Both the library and the documentation are young and are changing quickly, and it is inevitable that some things will be unclear. Accordingly, **this documentation is updated continuously** to address problems and omissions.

## The Setup

### Database Setup

The example code assumes a local [Redis] and/or [Redis Cluster] servers. You can set up the sample database using [docker] as follows:

```bash
$ docker pull grokzen/redis-cluster
$ docker run -d -t --name brick-redises \
    -p 7000:7000 \
    -p 7001:7001 \
    -p 7002:7002 \
    -p 7003:7003 \
    -p 7004:7004 \
    -p 7005:7005 \
    -p 7006:7006 \
    -p 7007:7007
```

Try a query or two to double-check your setup:

```sh
$ docker exec -it brick-redises /redis/src/redis-cli -p 7006 get key
(nil)
```

You can of course change this setup if you like, but you will need to adjust your Redis connection information accordingly.

### Scala Setup

On the Scala side you just need a console with the proper dependencies. A minimal `build.sbt` would look something like this:

```scala
scalaVersion := "2.12.4"
scalacOptions += "-Ypartial-unification" // 2.11.9+

lazy val brickVersion = "0.1.0"

libraryDependencies ++= Seq(
  "eu.monniot.brick" %% "brick-core"    % brickVersion,
  "eu.monniot.brick" %% "brick-lettuce" % brickVersion
)
```

The `-Ypartial-unification` compiler flag enables a bug fix that makes working with functional code significantly easier. See the Cats [Getting Started] for more info on this if it interests you.

If you are not using Lettuce you can omit `brick-lettuce` and will need to add the appropriate Redis driver as a dependency. Note that **brick** only provides driver for lettuce at this moment.

## Conventions

Each page begins with some imports, like this.

```scala
import cats._, cats.data._, cats.implicits._
import cats.effect._
import eu.monniot.brick._, eu.monniot.brick.implicits._
```

After that there is text interspersed with code examples. Sometimes definitions will stand alone

```scala
case class Person(name: String, age: Int)
val nel = NonEmptyList.of(Person("Bob", 42), Person("Alice", 21))
```

And sometimes they will appear as a REPL interaction

```repl
scala> nel.head
res0: Person = Person(Bob,42)

scala> nel.tail
res1: List[Person] = List(Person(Alice,21))
```

Sometimes we demonstrate that something doesn't compile. In such cases it will be clear from the context that this is expected, and not a problem with the documentation.

```repl
scala> woozle(nel) // doesn't compile
<console>:26: error: not found: value woozle
       woozle(nel) // doesn't compile
       ^
```

## Feedback and Contributions

Feedback on **brick** or this documentation is genuinely welcome. Please feel free to file a [pull request] if you have a contribution, or file an [issue].
