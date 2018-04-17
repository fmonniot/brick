// build.sc
import mill._
import mill.scalalib._
import mill.scalalib.publish.{PomSettings, License, Developer, SCM}
import ammonite.ops._

import $ivy.`org.scalameta:scalameta_2.12:3.5.0`
import $file.bintrayPublish

// TODO Add back cross compilation to scala 2.11

object core extends Cross[CoreModule]("2.12.5")

class CoreModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "fs2-redis-core"

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:1.0.1",
    ivy"org.typelevel::cats-effect:0.8",
    ivy"org.typelevel::cats-free:1.0.1"
  )

  def generateTestKitAlgebra() = T.command {
    import scala.meta._

    val source: Source = (millSourcePath / 'src / 'main / 'scala / 'eu / 'monniot / 'brick / 'free / "CommandsAlg.scala")
      .toIO.parse[Source]
      .getOrElse(throw new IllegalStateException("Cannot parse the CommandsAlg file"))

    // It assumes the template contains only a single extends,
    // and that extended type have only one parameterized type
    def extractReturnType(template: Template): Option[Type] = {
      template.inits.map(_.tpe).collectFirst {
        case Type.Apply(_, List(returnTpe)) => returnTpe
      }
    }


    val generated = source.stats.collectFirst {
      case q"package $_ { ..$stats }" =>

        val imp = q"import akka.actor.typed._"
        val actorCommandTrait = Type.Name("Command")
        val actorCommandTraitTerm = Term.Name("Command")

        val body = imp +: stats.map {
          case q"..$mods trait $_[..$_] extends $template" =>


            val newTemplateContent: List[Stat] = template.stats.flatMap {
              case q"..$mods class $tname[..$_] ..$_ (...$paramss) extends $template" =>

                for {
                  returnType <- extractReturnType(template)

                  replyToType: Type = Type.Apply(Type.Name("ActorRef"), List(returnType))
                  replyToTerm: Term.Param = param"replyTo: $replyToType"
                  responseParams: List[Term.Param] = paramss.flatten :+ replyToTerm

                  inits: List[Init] = List(Init(actorCommandTrait, actorCommandTraitTerm, List.empty))

                } yield q"..$mods class $tname (..$responseParams) extends ${template.copy(inits = inits)}"

              // We are replacing the original sealed trait by this one
              case q"sealed trait $tname[..$_]" if tname.syntax == "CommandOp" =>
                Option(q"sealed trait $actorCommandTrait")

              case other => Option(other)
            }

            q"..$mods trait RedisActorAlg extends ${template.copy(stats = newTemplateContent)}"
          case other =>
            other
        }

        q"package eu.monniot.brick.testkit {..$body}"
    }

    val target = pwd / 'testkit / 'src / 'main / 'scala / 'eu / 'monniot / 'brick / 'testkit / "RedisActorAlg.scala"

    write.over.apply(target, generated.get.toString())
  }

}

object lettuce extends Cross[LettuceModule]("2.12.5") {


}

class LettuceModule(val crossScalaVersion: String) extends CommonModule {

  override def moduleDeps = Seq(core())

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"io.lettuce:lettuce-core:5.0.2.RELEASE"
  )
}

object testkit extends Cross[TestKitModule]("2.12.5") {


}

class TestKitModule(val crossScalaVersion: String) extends CommonModule {

  override def moduleDeps = Seq(core())

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"com.typesafe.akka::akka-actor-typed:2.5.9"
  )
}

trait CommonModule extends CrossSbtModule with bintrayPublish.PublishToBintrayModule {

  val version = "0.1.0-SNAPSHOT"

  override def scalacPluginIvyDeps = Agg(
    ivy"org.spire-math::kind-projector:0.9.6"
  )

  override def scalacOptions = Seq(
    "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
    "-language:higherKinds", // Allow higher-kinded types
    "-language:implicitConversions", // Allow definition of implicit functions called views
    "-Ypartial-unification", // Enable partial unification in type constructor inference
    "-deprecation",
    "-feature"
  )

  object test extends Tests {
    override def ivyDeps = Agg(
      ivy"org.scalatest::scalatest:3.0.4"
    )

    def testFrameworks = Seq("org.scalatest.tools.Framework")
  }

  def pomSettings = PomSettings(
    description = artifactName(),
    organization = "eu.monniot.fs2",
    url = "https://github.com/fmonniot/fs2-redis",
    licenses = Seq(
      License("Apache 2.0", "https://opensource.org/licenses/Apache-2.0")
    ),
    scm = SCM(
      "git://github.com/fmonniot/fs2-redis.git",
      "scm:git://github.com/fmonniot/fs2-redis.git"
    ),
    developers = Seq(
      Developer("fmonniot", "François Monniot", "https://francois.monniot.eu")
    )
  )

  def publishVersion = {
    if (version.endsWith("-SNAPSHOT")) {
      import ammonite.ops.ImplicitWd._
      val commit = %%("git", "rev-parse", "HEAD").out.lines.mkString
      version.replace("SNAPSHOT", commit)
    } else version
  }

  override def bintrayRepository = T {
    if (version.endsWith("-SNAPSHOT")) "snapshots" else "maven"
  }

}
