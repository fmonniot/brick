---
doc: true
title: Commanding
---
# Chapter 3: Commanding

In this chapter we will write some programs to issue commands to _Redis_.

## Setting Up

First let's get our imports out of the way and set up a `Transactor` as we did before. You can skip this step if you still have your REPL running from the last chapter.

```scala
import eu.monniot.brick._
import cats._, cats.data._, cats.implicits._
import cats.effect.IO

val xa = lettuce.Transactor.fromRedisClient("redis://localhost:7006/0")
```

## Request-Response Commands

The vast majority of the commands one can issue to Redis are made on a request - response model; meaning you issue a command and then get a response.

This kind of commands are defined as pure function in the `eu.monniot.brick.commands` package and mirror the redis commands name. That mean the _del_ redis command is mirrored with the `del(keys: NonEmptyList[String]): CommandIO[Long]` function.

Those commands can be executed with the `Transactor.exec` natural transformation.

If you need to make multiple commands in an atomic way, `Transactor` offer the `multi` transformation to wrap the program with a _MULTI_/_EXEC_ redis structure.

## Streaming Commands

Redis also offers a stream interface, notably in the form of the _Pub/Sub_ features. To support those, we are based on the `fs2.Stream` type.

TODO

## Administrative Commands

TODO
like `cluster info`

## Redis Cluster

TODO
