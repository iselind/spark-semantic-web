ThisBuild / onLoad := {
  val previous = (ThisBuild / onLoad).value
  state => {
    val v = System.getProperty("java.specification.version").toDouble
    if (v < 17.0)
      sys.error(s"""|ERROR: Spark-4 requires JDK-17 or newer.
              |Current JVM = $v
              |Please launch sbt with:
              |   JAVA_HOME=/path/to/jdk-17 sbt
              |""".stripMargin)
    previous(state)
  }
}

val scala213 = "2.13.16"
val sparkVer = "4.0.0"

lazy val root = project
  .in(file("."))
  .settings(
    name := "spark4x-playground",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala213,
    libraryDependencies ++= Seq(
      // Spark SQL + its transitive core libs
      "org.apache.spark" %% "spark-sql" % sparkVer,
      // Jena parser & query engine
      "org.apache.jena" % "apache-jena-libs" % "4.10.0" pomOnly (),
      // MUnit test framework
      "org.scalameta" %% "munit" % "1.0.0" % Test
    ),

    // Use the JVM in which SBT runs (17+ recommended for Spark-4)
    javaHome := sys.props.get("java.home").map(file)
  )
