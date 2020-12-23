import sbt._
import Keys._

lazy val commonSettings = Seq(
  organization := "com.void",
  version := "1.0.0"
)

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(name := "project-b-root")
  .aggregate(`project-b`)

lazy val `project-b`: Project = project
  .in(file("project-b"))
  .settings(commonSettings)
  .settings(
    name := "project-b",
    description := "Project B",
    libraryDependencies ++= Seq(
        "org.apache.avro" % "avro" % "1.10.1" % "provided"
    )
  )