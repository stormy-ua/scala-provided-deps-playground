import sbt._
import Keys._

lazy val commonSettings = Seq(
  organization := "com.void",
  version := "1.0.0"
)

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(name := "project-a-root")
  .aggregate(`project-a`)

lazy val `project-a`: Project = project
  .in(file("project-a"))
  .settings(commonSettings)
  .settings(
    name := "project-a",
    description := "Project A",
    libraryDependencies ++= Seq(
        "org.apache.avro" % "avro" % "1.8.1" % "provided"
    )
  )