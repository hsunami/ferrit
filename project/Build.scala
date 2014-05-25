import sbt._
import Keys._

object B extends Build {

  lazy val scalaTest = "org.scalatest" %% "scalatest" % "1.9.1" % "it,test"

  lazy val root =
    Project("root", file("."))
      .configs(IntegrationTest)
      .settings(Defaults.itSettings : _*)
      .settings(libraryDependencies += scalaTest)

}