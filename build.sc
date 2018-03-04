// build.sc
import mill._
import mill.scalalib._
import mill.scalalib.publish.{PomSettings, License, Developer, SCM}
import ammonite.ops._

import $file.bintrayPublish

// TODO Add back cross compilation to scala 2.11

object core extends Cross[CoreModule]("2.12.4")

class CoreModule(val crossScalaVersion: String) extends CommonModule {

  override def artifactName = "fs2-redis-core"

  override def ivyDeps = Agg(
    ivy"org.typelevel::cats-core:1.0.1",
    ivy"org.typelevel::cats-effect:0.8",
    ivy"org.typelevel::cats-free:1.0.1"
  )

}

object lettuce extends Cross[LettuceModule]("2.12.4") {


}

class LettuceModule(val crossScalaVersion: String) extends CommonModule {

  override def moduleDeps = Seq(core())

  override def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"io.lettuce:lettuce-core:5.0.2.RELEASE"
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
