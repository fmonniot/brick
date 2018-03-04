# Brick

[![Build Status](https://travis-ci.org/fmonniot/brick.svg?branch=master)](https://travis-ci.org/fmonniot/brick)
![Project Status](https://img.shields.io/badge/project%20status-experimental-yellowgreen.svg)

> Like doobie, but for Redis.

Brick is an open-source library which provides a set of tools to integrate Redis into the `cats` and `cats-effects` world.
It currently provides an implementation for the [lettuce library](https://lettuce.io)

```scala
import cats.effect.IO
import com.datastax.driver.core.Session
import eu.monniot.brick
import eu.monniot.brick.lettuce.Transactor
import eu.monniot.brick.commands._ // TODO Fix this import


// Create the redis session (or reuse a lettuce RedisClient)
implicit val transact = Transactor.fromRedisClient("redis://password@localhost:6379/0")

val getAndExpire: CommandIO[String] = for {
    v <- get("myKey")
    _ <- expire("myKey")
} yield v

// Returns an effect monad which will execute our redis commands
val result: IO[String] = getAndExpire.exec

val transaction: CommandIO[String] = for {
    v <- hget("myKey", "myField")
    v2 = s"hello $v" // do some useful work :)
    _ <- hset("myKey", "myField", v2)
} yield v2

// Returns an effect monad with our transactions commands executed in a MULTI/EXEC block
val result: IO[String] = transaction.multi
```

## Quickstart with sbt

Brick is published to maven Central and cross built against scala 2.12.4 and 2.11.12,
 so you can just add the following to your build:

```scala
libraryDependencies += "eu.monniot.brick" %% "brick-core" % "0.1.0"
libraryDependencies += "eu.monniot.brick" %% "brick-lettuce" % "0.1.0"
```

## Documentation

The Brick documentation is available at https://francois.monniot.eu/brick.

## Contributing

The Brick project welcomes contributions from anybody wishing to
participate.  All code or documentation that is provided must be
licensed with the same license that Brick is licensed with (Apache
2.0, see LICENSE.txt).

People are expected to follow the
[Typelevel Code of Conduct](https://typelevel.org/conduct.html) when
discussing Brick on the Github page or other venues.

Feel free to open an issue if you notice a bug, have an idea for a
feature, or have a question about the code. Pull requests are also
gladly accepted. For more information, check out the
[contributor guide](CONTRIBUTING.md).

## License

All code in this repository is licensed under the Apache License,
Version 2.0.  See [LICENCE.txt](LICENSE.txt).