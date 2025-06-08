val scalaVer = "2.12.18"
val sparkVersion = "3.5.1"

lazy val commonSettings = Seq(
  scalaVersion := scalaVer,
  organization := "com.yourorg",
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
  version := "0.1.0-SNAPSHOT",
)

lazy val formatOnCompileSettings = Seq(
  Test / compile := (Test / compile).dependsOn(Compile / scalafmt, Test / scalafmt).value
)

lazy val library = (project in file("library"))
  .settings(
    commonSettings,
    formatOnCompileSettings,
    name := "your-library",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % sparkVersion,
      "org.apache.spark" %% "spark-sql" % sparkVersion,
      "org.apache.jena" % "apache-jena-libs" % "4.10.0" pomOnly()
    )
  )

lazy val app = (project in file("app"))
  .dependsOn(library)
  .settings(
    commonSettings,
    formatOnCompileSettings,
    name := "your-app",
  )

lazy val root = (project in file("."))
  .aggregate(library, app)
  .settings(
    commonSettings,
    name := "spark-semantic-web",
    publish / skip := true
  )

enablePlugins(ScalafmtPlugin)
addCommandAlias("format", "scalafmtAll")
addCommandAlias("checkFormat", "scalafmtCheckAll")
