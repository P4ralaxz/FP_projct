val scala3Version = "3.8.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "projext",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4"
    )
  )
