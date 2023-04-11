import mill._
import mill.scalalib._
import mill.scalalib.publish._
import scalafmt._

import $ivy.`com.lihaoyi::mill-contrib-scoverage:`
import mill.contrib.scoverage.{ScoverageModule, ScoverageReport}
import $ivy.`io.chris-kipp::mill-ci-release::0.1.5`
import io.kipp.mill.ci.release.{CiReleaseModule, SonatypeHost}

object versions {
  val scala     = "3.3.0-RC3"
  val zio       = "2.0.11"
  val scoverage = "2.0.8"
}

trait Base extends ScalaModule {
  def ivyDeps = Agg(
    ivy"dev.zio::zio:${versions.zio}",
  )
  def scalacOptions = Seq("-source:future")
  def scalaVersion  = versions.scala
}
object ziochannel
  extends Base
  with ScalafmtModule
  with ScoverageModule
  with CiReleaseModule {
  def scoverageVersion = versions.scoverage
  def pomSettings = PomSettings(
    description = "Ziochannel is a Go-like channel implementation for ZIO",
    organization = "com.carlosedp",
    url = "https://github.com/carlosedp/ziochannel",
    licenses = Seq(License.MIT),
    versionControl = VersionControl.github("carlosedp", "ziochannel"),
    developers = Seq(
      Developer("carlosedp", "Carlos Eduardo de Paula", "https://github.com/carlosedp"),
    ),
  )
  override def sonatypeHost = Some(SonatypeHost.s01)

  object test extends Tests with ScoverageTests {
    def ivyDeps = Agg(
      ivy"dev.zio::zio-test:${versions.zio}",
      ivy"dev.zio::zio-test-sbt:${versions.zio}",
    )
    def testFramework = T("zio.test.sbt.ZTestFramework")
  }
}

object examples extends Base {
  def moduleDeps = Seq(ziochannel)
}

object scoverage extends ScoverageReport {
  override def scalaVersion     = versions.scala
  override def scoverageVersion = versions.scoverage
}
// -----------------------------------------------------------------------------
// Command Aliases
// -----------------------------------------------------------------------------
// Alias commands are run like `./mill run [alias]`
// Define the alias as a map element containing the alias name and a Seq with the tasks to be executed
val aliases: Map[String, Seq[String]] = Map(
  "fmt"      -> Seq("mill.scalalib.scalafmt.ScalafmtModule/reformatAll __.sources"),
  "checkfmt" -> Seq("mill.scalalib.scalafmt.ScalafmtModule/checkFormatAll __.sources"),
  "deps"     -> Seq("mill.scalalib.Dependency/showUpdates"),
  "pub"      -> Seq("io.kipp.mill.ci.release.ReleaseModule/publishAll"),
  "publocal" -> Seq("ziochannel.publishLocal"),
  "testall"  -> Seq("__.test"),
  "coverage" -> Seq(s"__.test", "scoverage.htmlReportAll", "scoverage.xmlReportAll", "scoverage.consoleReportAll"),
)

def run(ev: eval.Evaluator, alias: String = "") = T.command {
  aliases.get(alias) match {
    case Some(t) =>
      mill.main.MainModule.evaluateTasks(ev, t.flatMap(x => Seq(x, "+")).flatMap(_.split("\\s+")).init, false)(identity)
    case None =>
      Console.err.println("Use './mill run [alias]'."); Console.out.println("Available aliases:")
      aliases.foreach(x => Console.out.println(s"${x._1.padTo(15, ' ')} - Commands: (${x._2.mkString(", ")})"));
      sys.exit(1)
  }
}
