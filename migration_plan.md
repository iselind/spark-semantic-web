# ✅ SPARQL Refactor Migration Checklist

This checklist outlines the steps to modularize Jena support from the `library` into a standalone `jena-support` module, and introduce a clean plugin-style architecture.

---

## 📚 Stage 8: Documentation & Final Polish

- Add `README.md` to `sparql.core.ext/`:
  - Describe purpose of traits
  - List required traits to implement a new engine
  - Link to `sparql.jena` as a reference implementation
- (Optional) Add `EXTENDING.md` guide with trait descriptions & wiring pattern
- Add `README.md` to jena-support
  - What this module provides (a concrete Jena implementation of the sparql.core.ext traits)
  - How to use it in sbt:
    ```
    libraryDependencies += "com.example" %% "jena-support" % "1.0.0"
    ```
  - How to wire it
    ```
    import sparql.jena.JenaSupport
    val engine = JenaSupport.defaultEngine()
    ```
  - Any transitive dependencies (e.g., Spark, Jena versions) to be aware of
  - Link to `sparql.core.ext` for trait-level documentation

✅ *Goal: Clear contributor story + modular design knowledge captured.*

---

## 🧼 Stage 9: Verification by clean build

- Check sbt `clean compile test` for all modules
- Confirm classpath is correct (no hidden Jena in `library`)

✅ *Goal: Everything modular, documented, tested, and ready for distribution.*

---

## 📚 Stage 10: Enforcing architecture
- Add Scalafix to project/plugins.sbt
  ```
  addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.10.4")

  ThisBuild / scalafixDependencies += "com.github.liancheng" %% "organize-imports" % "0.6.0"
  ThisBuild / semanticdbEnabled := true
  ThisBuild / scalacOptions += "-Yrangepos"
  ```
- Create .scalafix.conf to disallow forbidden imports
  ```
    rules = [
    Disable.import
  ]
  
  Disable.imports = [
    "org.apache.jena.*",
    "sparql.jena.*"
  ]
  ```
- Add verify task to each non-root module in build.sbt
  ```
  lazy val verifySettings = Seq(
  verify := {
      val _ = (Test / test).value
      val _ = (scalafix.toTask(" --check")).value
    }
  )
  
  // You can now mix that in using something like
  // lazy val library = project.in(file("library"))
  //   .settings(commonSettings, verifySettings, name := "library")

  addCommandAlias("verifyAll", ";clean;verify")
  ```
- Make sure each subproject enables Scalafix (enablePlugins(ScalafixPlugin)).
- Add verify task to root in build.sbt
  ```
  lazy val root = (project in file("."))
  .aggregate(app, library, jenaSupport)
  .settings(
    verify := {
      val _ = (app / verify).value
      val _ = (library / verify).value
      val _ = (jenaSupport / verify).value
    }
  )
  ```
- Run scalafix on the repo and validate results, `sbt verify`
- Integrate in CI/test execution
- Document the rules in CONTRIBUTING.md or README.md

✅ *Goal: Ensure the architecture stays clean.*
