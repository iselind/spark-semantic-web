import sbt.Keys.resolvers

val scalaVer = "2.12.15"
val sparkVersion = "3.5.1"

lazy val commonSettings = Seq(
  scalaVersion := scalaVer,
  organization := "com.example",
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
  version := "0.1.0-SNAPSHOT"
)

lazy val formatOnCompileSettings = Seq(
  Test / compile := (Test / compile)
    .dependsOn(Compile / scalafmt, Test / scalafmt)
    .value
)

lazy val jenaSupport = (project in file("jena-support"))
  .dependsOn(library)
  .settings(
    commonSettings,
    formatOnCompileSettings,
    name := "jena-support",
    libraryDependencies ++= Seq(
      "org.apache.jena" % "apache-jena-libs" % "4.10.0" pomOnly()
    )
  )

lazy val library = (project in file("library"))
  .settings(
    commonSettings,
    formatOnCompileSettings,
    name := "sparkql",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.spark" %% "spark-graphx" % sparkVersion,
      "graphframes" % "graphframes" % "0.8.4-spark3.5-s_2.12"
    ),
    resolvers += "Spark Packages Repo" at "https://repos.spark-packages.org/"
  )

lazy val app = (project in file("app"))
  .dependsOn(jenaSupport)
  .settings(
    commonSettings,
    formatOnCompileSettings,
    name := "your-app"
  )

lazy val root = (project in file("."))
  .aggregate(library, app, jenaSupport)
  .settings(
    commonSettings,
    name := "spark-semantic-web",
    publish / skip := true
  )

enablePlugins(ScalafmtPlugin)
addCommandAlias("format", "scalafmtAll")
addCommandAlias("checkFormat", "scalafmtCheckAll")
