import sbt.Keys.resolvers

val scalaVer = "2.13.16"
val sparkVersion = "4.0.0"

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
      "org.apache.spark" %% "spark-sql" % sparkVersion,
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
enablePlugins(ScalafixPlugin)

inThisBuild(
  List(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    scalaVersion := scalaVer,
    scalacOptions ++= List(
      "-Yrangepos", // required for scalafix semantic rules
      "-Wunused:imports",  // warns about unused imports
      "-Wunused:privates", // optional, for unused private vals/methods
      "-Wunused:locals"    // optional, for unused local variables
    )
  )
)
