import java.math.BigInteger
import java.security.MessageDigest

import ammonite.ops._
import mill.define.{ExternalModule, Task}
import mill.eval.PathRef
import mill.modules.Jvm.createJar
import mill.scalalib.publish.{Artifact, PatientHttp}
import mill.scalalib.{Lib, ScalaModule}
import mill.util.Logger
import mill.{T, define}
import scalaj.http.{HttpOptions, HttpResponse}

import scala.util.Try
import scala.concurrent.duration._


trait PublishToBintrayModule extends PublishBaseModule {

  def bintrayCredentialsPath: T[Path] = T(home / ".bintray" / ".credentials")

  /**
    * The Bintray repository to push the module to.
    */
  def bintrayRepository: T[String] = T("maven")

  /**
    * The Bintray package name. It defaults to the module name.
    */
  def bintrayPackage: T[String] = T {
    val segments = millModuleSegments.value

    segments.head.pathSegments.head
  }


  /**
    * Publish the current project as a maven artifact to a Bintray account.
    *
    * We voluntary do not let people pass their bintray credentials from the command line.
    * Instead, we expect people to use either:
    * - Java properties (this one is from SBT and it would make sense to switch to CLI args)
    * - Environment variables (BINTRAY_USER and BINTRAY_PASS)
    * - The bintray properties file (path configurable in the build description)
    *
    * The Java properties way may be remove in the future, as it means the credentials are still
    * available in the command executed. Or we may use CLI arguments instead.
    *
    * @param release Whether the new uploaded artifact should be published as well
    * @param sign    Whether a signature file should be generated and uploaded as well
    */
  // TODO Can we use Option here instead of null to indicates an argument is optional ?
  // This would be really useful for gpg passphrase or bintray credentials
  def publishMaven(release: Boolean = true, sign: Boolean = false): define.Command[Unit] = T.command {
    val credentials = BintrayCredentials.read(bintrayCredentialsPath())
    val PublishBaseModule.PublishData(artifactInfo, artifacts) = publishArtifacts()

    new BintrayPublisher(bintrayRepository(), bintrayPackage(), credentials, T.ctx().log)
      .publishMaven(artifacts.map { case (a, b) => (a.path, b) }, artifactInfo, release, sign)
  }

}

object PublishToBintrayModule extends ExternalModule {
  def millDiscover: mill.define.Discover[this.type] = mill.define.Discover[this.type]
}


case class BintrayCredentials(user: String, pass: String, gpgPassphrase: Option[String] = None)

object BintrayCredentials {

  def read(path: Path): BintrayCredentials =
    propsCredentials
      .orElse(envCredentials)
      .orElse(fileCredentials(path))
      .getOrElse(throw new IllegalStateException("Publishing to bintray requires you to pass a username and api key"))

  private def propsCredentials =
    for {
      name <- sys.props.get("bintray.user")
      pass <- sys.props.get("bintray.pass")
      gpg = sys.props.get("gpg.passphrase")
    } yield BintrayCredentials(name, pass, gpg)

  private def envCredentials =
    for {
      name <- sys.env.get("BINTRAY_USER")
      pass <- sys.env.get("BINTRAY_PASS")
      gpg = sys.env.get("GPG_PASSPHRASE")
    } yield BintrayCredentials(name, pass, gpg)

  private def fileCredentials(path: Path) = {
    path match {
      case creds if creds.toIO.exists() =>
        for {
          mapped <- readPropertiesFile(path)
          user <- mapped.get("user")
          pass <- mapped.get("password")
          gpg = mapped.get("gpg.passphrase")
        } yield BintrayCredentials(user, pass, gpg)

      case _ => None
    }
  }

  private def readPropertiesFile(creds: Path) = Try {
    import scala.collection.JavaConverters._

    val props = new java.util.Properties
    props.load(ammonite.ops.read.getInputStream(creds))

    props.asScala.map {
      case (k, v) => (k.toString, v.toString.trim)
    }.toMap
  }.toOption
}


class BintrayPublisher(repository: String,
                       pkg: String,
                       credentials: BintrayCredentials,
                       log: Logger) {

  import mill.scalalib.publish._

  private val api = new BintrayHttpApi(repository, pkg, credentials, log)

  // Publish maven style the given artifact
  def publishMaven(fileMapping: Seq[(Path, String)], artifact: Artifact, release: Boolean, sign: Boolean): Unit =
    publishMavenAll(release, sign, fileMapping -> artifact)

  def publishMavenAll(release: Boolean, sign: Boolean, artifacts: (Seq[(Path, String)], Artifact)*): Unit = {
    val mappings = for ((mapping, artifact) <- artifacts) yield {
      val publishPath = Seq(
        artifact.group.replace(".", "/"),
        artifact.id,
        artifact.version
      ).mkString("/")

      log.info(s"Creating mapping for $publishPath")

      val fileMapping = mapping.map { case (file, name) => (file, publishPath + "/" + name) }

      val signedArtifacts = fileMapping ++ fileMapping.collect {
        case (file, name) if sign => poorMansSign(file, credentials.gpgPassphrase) -> s"$name.asc"
      }

      artifact -> signedArtifacts.flatMap { case (path, name) =>

        val content = read.bytes(path)

        Seq(
          name -> content,
          (name + ".md5") -> md5hex(content),
          (name + ".sha1") -> sha1hex(content)
        )
      }
    }

    val results = mappings.map { case (artifact, payloads) =>
      val uploadResults = payloads.map { case (fileName, data) =>
        log.info(s"Uploading $fileName to bintray")
        api.mavenUpload(fileName, data)
      }

      val publishResult = if (release) {
        log.info(s"Releasing $pkg version ${artifact.version} on Bintray")
        Option(api.publish(artifact.version))
      } else None

      artifact -> (uploadResults ++ publishResult)
    }

    reportResults(results)
  }

  private def reportResults(results: Seq[(Artifact, Seq[HttpResponse[String]])]): Unit = {

    val (ok, failed) = results.partition(_._2.forall(_.is2xx))

    if (ok.nonEmpty) {
      log.info(s"Published ${ok.map(_._1.id).mkString(", ")} to Bintray")
    }
    if (failed.nonEmpty) {
      throw new RuntimeException(
        failed
          .map { case (artifact, responses) =>
            val errors = responses.filterNot(_.is2xx).map { response =>
              s"Code: ${response.code}, message: ${response.body}"
            }

            s"Failed to publish ${artifact.id} to Bintray. Errors: \n${errors.mkString("\n")}"
          }
          .mkString("\n")
      )
    }
  }

  // http://central.sonatype.org/pages/working-with-pgp-signatures.html#signing-a-file
  // Assuming it's the same for Bintray, as they let you sync to Maven Central afterwards
  // If needed we can offer the possibility of doing it via the Bintray API
  private def poorMansSign(file: Path, passphrase: Option[String]): Path = {
    val fileName = file.toString
    import ammonite.ops.ImplicitWd._

    val args = List("gpg", "--yes", "-a", "-b", "--batch") ++
      passphrase.map(pp => List("--passphrase", pp)).getOrElse(List.empty) :+
      fileName

    %(args)
    Path(fileName + ".asc")
  }

  private def md5hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(md5.digest(bytes)).getBytes

  private def sha1hex(bytes: Array[Byte]): Array[Byte] =
    hexArray(sha1.digest(bytes)).getBytes

  private def md5 = MessageDigest.getInstance("md5")

  private def sha1 = MessageDigest.getInstance("sha1")

  private def hexArray(arr: Array[Byte]) =
    String.format("%0" + (arr.length << 1) + "x", new BigInteger(1, arr))

}

class BintrayHttpApi(bintrayRepository: String,
                     bintrayPackage: String,
                     credentials: BintrayCredentials,
                     log: Logger) {

  private val subject = credentials.user

  private val baseUri = "https://bintray.com/api/v1"

  private val uploadTimeout = 5.minutes.toMillis.toInt

  // PUT /maven/:subject/:repo/:package/:file_path[;publish=0/1]
  def mavenUpload(filePath: String,
                  data: Array[Byte]): HttpResponse[String] = {
    val uri = s"$baseUri/maven/$subject/$bintrayRepository/$bintrayPackage/$filePath"

    PatientHttp(uri)
      .option(HttpOptions.readTimeout(uploadTimeout))
      .method("PUT")
      .auth(credentials.user, credentials.pass)
      .header("Content-Type", "application/binary")
      .put(data)
      .asString
  }


  // POST /content/:subject/:repo/:package/:version/publish
  def publish(version: String): HttpResponse[String] = {
    val uri = s"$baseUri/content/$subject/$bintrayRepository/$bintrayPackage/$version/publish"

    PatientHttp(uri)
      .option(HttpOptions.readTimeout(uploadTimeout))
      .method("POST")
      .auth(credentials.user, credentials.pass)
      .header("Content-Type", "application/json")
      .asString
  }
}


/**
  * Configuration necessary for publishing a Scala module to Maven Central or similar
  */
trait PublishToSonatypeModule extends PublishBaseModule {
  outer =>

  import mill.scalalib.publish._

  def sonatypeUri: String = "https://oss.sonatype.org/service/local"

  def sonatypeSnapshotUri: String = "https://oss.sonatype.org/content/repositories/snapshots"

  def publish(sonatypeCreds: String,
              gpgPassphrase: String,
              release: Boolean): define.Command[Unit] = T.command {
    val PublishBaseModule.PublishData(artifactInfo, artifacts) = publishArtifacts()
    new SonatypePublisher(
      sonatypeUri,
      sonatypeSnapshotUri,
      sonatypeCreds,
      gpgPassphrase,
      T.ctx().log
    ).publish(artifacts.map { case (a, b) => (a.path, b) }, artifactInfo, release)
  }

}

object PublishToSonatypeModule extends ExternalModule {

  import mill.scalalib.publish._

  def publishAll(sonatypeCreds: String,
                 gpgPassphrase: String,
                 publishArtifacts: mill.main.Tasks[PublishBaseModule.PublishData],
                 release: Boolean = false,
                 sonatypeUri: String = "https://oss.sonatype.org/service/local",
                 sonatypeSnapshotUri: String = "https://oss.sonatype.org/content/repositories/snapshots") = T.command {

    val x: Seq[(Seq[(Path, String)], Artifact)] = Task.sequence(publishArtifacts.value)().map {
      case PublishBaseModule.PublishData(a, s) => (s.map { case (p, f) => (p.path, f) }, a)
    }
    new SonatypePublisher(
      sonatypeUri,
      sonatypeSnapshotUri,
      sonatypeCreds,
      gpgPassphrase,
      T.ctx().log
    ).publishAll(
      release,
      x: _*
    )
  }

  def millDiscover: mill.define.Discover[this.type] = mill.define.Discover[this.type]

}


/**
  * Configuration necessary for publishing a Scala module.
  * This is taken as is from the mill source, with some changes:
  * - the publish method extracted to PublishTo* traits
  * - the publishLocal method renamed into publishIvyLocal
  * - added a publishMavenLocal command
  */
trait PublishBaseModule extends ScalaModule {
  outer =>

  import mill.scalalib.publish._

  override def moduleDeps = Seq.empty[PublishBaseModule]

  def pomSettings: T[PomSettings]

  def publishVersion: T[String]

  def artifactId: T[String] = T {
    s"${artifactName()}${artifactSuffix()}"
  }

  def publishSelfDependency = T {
    Artifact(pomSettings().organization, artifactId(), publishVersion())
  }

  def publishXmlDeps = T.task {
    val ivyPomDeps = ivyDeps().map(
      Artifact.fromDep(_, scalaVersion(), Lib.scalaBinaryVersion(scalaVersion()))
    )
    val modulePomDeps = Task.sequence(moduleDeps.map(_.publishSelfDependency))()
    ivyPomDeps ++ modulePomDeps.map(Dependency(_, Scope.Compile))
  }

  def pom = T {
    val pom = Pom(artifactMetadata(), publishXmlDeps(), artifactId(), pomSettings())
    val pomPath = T.ctx().dest / s"${artifactId()}-${publishVersion()}.pom"
    write.over(pomPath, pom)
    PathRef(pomPath)
  }

  def ivy = T {
    val ivy = Ivy(artifactMetadata(), publishXmlDeps())
    val ivyPath = T.ctx().dest / "ivy.xml"
    write.over(ivyPath, ivy)
    PathRef(ivyPath)
  }

  def artifactMetadata: T[Artifact] = T {
    Artifact(pomSettings().organization, artifactId(), publishVersion())
  }

  def publishArtifacts = T {
    val baseName = s"${artifactId()}-${publishVersion()}"
    PublishBaseModule.PublishData(
      artifactMetadata(),
      Seq(
        jar() -> s"$baseName.jar",
        sourceJar() -> s"$baseName-sources.jar",
        docJar() -> s"$baseName-javadoc.jar",
        pom() -> s"$baseName.pom"
      )
    )
  }

  def publishIvyLocal(): define.Command[Unit] = T.command {
    LocalPublisher.publish(
      jar = jar().path,
      sourcesJar = sourceJar().path,
      docJar = docJar().path,
      pom = pom().path,
      ivy = ivy().path,
      artifact = artifactMetadata()
    )
  }

  def publishMavenLocal(): define.Command[Unit] = T.command {
    // TODO
  }


  // Override the docJar task to return the regular jar file
  // This is because the documentation is currently broken (the ScalaDoc
  // class isn't in the classpath, probably because the compile isn't
  // in it) so for the time being, no doc jar :(
  // TODO
  /*override def docJar = T {
    createJar(
      (resources() ++ Seq(compile().classes)).map(_.path).filter(exists),
      mainClass()
    )
  }
  */

}

object PublishBaseModule {

  case class PublishData(meta: Artifact, payload: Seq[(PathRef, String)])

  object PublishData {
    implicit def jsonify: upickle.default.ReadWriter[PublishData] = upickle.default.macroRW
  }

}