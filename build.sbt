val scalaVer = "2.12.18"
val sparkVersion = "3.5.1"

lazy val commonSettings = Seq(
  scalaVersion := scalaVer,
  organization := "com.yourorg",
  libraryDependencies += "org.scalameta" %% "munit" % "1.0.0" % Test,
  version := "0.1.0-SNAPSHOT"
)

lazy val library = (project in file("library"))
  .settings(
    commonSettings,
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
    name := "your-app",

    // To get Spark to work in Java 11+
    run / fork := true,
    run / javaOptions += "--add-exports=java.base/sun.nio.ch=ALL-UNNAMED"
  )

lazy val root = (project in file("."))
  .aggregate(library, app)
  .settings(
    commonSettings,
    name := "your-project",
    publish / skip := true
  )