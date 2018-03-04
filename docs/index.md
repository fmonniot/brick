# Brick
Build Badge
Maven Central Version Badge
Javadoc Badge (javadoc.io)

> Right sided image: brick logo

Brick is a pure functional Redis layer for [Scala] and [Cats].
Brick provides a minimal but expressive API:

```scala
import cats.effect.IO
import eu.monniot.brick.lettuce.Transactor
import eu.monniot.brick.commands._ // TODO Fix this import

val transact =
    Transactor.fromRedisClient("redis://password@localhost:6379/0")

def computation(key: String, f: String => String): CommandIO[String] = for {
    v <- get(key)
    v2 = f(v)
    _ <- set(key, v2)
} yield v2
```

And then

```scala

scala> set("myKey", "brick").exec.unsafeRunSync()
res1: String = brick

scala> computation("myKey", _.capitalize).multi.unsafeRunSync()
res2: String = Brick
```

## Quick Start

The current version is *0.1.0* for Scala 2.12 with

- [cats] 1.0.1
- [cats-effect] 0.9
- [fs2] 0.10.2

To use **brick** you need to add the following to your `build.sbt`:

```scala

scalacOptions += "-Ypartial-unification" // 2.11.9+

libraryDependencies ++= Seq(
    // Always add this one
    "eu.monniot.brick" %% "brick-core" % "0.1.0",

    // And those as needed
    "eu.monniot.brick" %% "brick-lettuce" % "0.1.0", // Lettuce driver
    "eu.monniot.brick" %% "brick-test-kit" % "0.1.0" // The Brick test-kit
)
```

Note that **brick** is pre-1.0 software and is still undergoing active development.
New versions are **not** binary compatible with prior versions, although in most
cases user code will be source compatible.

## Documentation and Support

- Behold the sparkly [documentation] ← Start here
- The [Scaladoc] will be handy once you get your feet wet
- See the [changelog] for an overview of changes
- There is also the [source]. Check the examples too.
- If you have comments or run into troubles, please file an issue.

## Testing

If you want to build and run the tests for yourself, you'll need both a local redis and a redis cluster. You can see the `before_script` section of the [`.travis.yml`] file for an up-to-date list of steps for preparing the tests.

## Acknowledgment

Brick started as an experiment to better understand [**doobie**]. Turns out it's actually something that can be useful too, so the library is available for all.
I'd like to thanks @tpolecat for creating the amazing **doobie** library and its awesome documentation (which I have shamely copied and adapted here).
