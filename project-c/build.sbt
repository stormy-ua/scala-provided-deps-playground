import sbt._
import Keys._

lazy val commonSettings = Seq(
  organization := "com.void"
)

version := "1.0.0"

lazy val root: Project = project
  .in(file("."))
  .settings(commonSettings)
  .settings(name := "project-c-root")
  .aggregate(`project-c`)

lazy val `project-c`: Project = project
  .in(file("project-c"))
  .settings(commonSettings)
  .settings(
    name := "project-c",
    description := "Project C",
    libraryDependencies ++= Seq(
        "com.void" %% "project-a" % "1.0.0",
        "com.void" %% "project-b" % "1.0.0",
        "org.apache.avro" % "avro" % "1.8.1",
        //"org.apache.avro" % "avro" % "1.10.1"
    )
  )