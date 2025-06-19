# ✅ SPARQL Refactor Migration Checklist

This checklist outlines the steps to modularize Jena support from the `library` into a standalone `jena-support` module, and introduce a clean plugin-style architecture.

---

## 🧱 Stage 1: Restructure Package Layout in `library`

- Rename package `sparql` → `sparql.core`
- Create new packages:
  - `sparql.core.ext` ← for abstraction traits
  - `sparql.jena`     ← for current Jena-based implementations (initially remains in `library`)
- Update all existing import paths
- Ensure all code compiles and tests pass

✅ *Goal: Setup future structure without affecting functionality.*

---

## 🧩 Stage 2: Define Extensibility Layer

- In `sparql.core.ext`, define the following (start minimal):
  - `RDFTriple`
  - `RDFModel`
  - `RDFLoader`
  - `SparqlEngine`
  - Any needed factories or typeclasses
- In `sparql.jena`, implement these traits using Jena
- Refactor any `core` logic to use traits instead of Jena directly
- Ensure no Jena types leak into `core` public APIs

✅ *Goal: All Jena usage lives behind defined interfaces.*

---

## 🔁 Stage 3: Extract Jena-Specific Logic

- Identify all classes that depend on Jena
  - `grep -Rns "org.apache.jena" library/src app/src`
- One-by-one:
  - Move Jena-specific logic to `sparql.jena`
  - Replace use in `sparql.core` with abstract traits
  - Update internal tests accordingly
- Validate functionality remains correct

✅ *Goal: `sparql.core` is now fully backend-agnostic.*

---

## 📦 Stage 4: Create `jena-support` Module

- Add new sbt module: `jena-support/`
- Add dependency: `jena-support` → `library`
- Add dependency: `jena-support` → `Apache Jena`
- Copy `sparql.jena` package into `jena-support` (temporarily duplicate)
- Ensure `jena-support` compiles independently
- Validate basic usage from `jena-support` test or REPL

✅ *Goal: Jena support is now independently buildable.*

---

## 🔨 Stage 5: Move `sparql.jena` out of `library`
- Delete `sparql.jena` from `library`
- Ensure no broken references in `sparql.core`
- Fix any remaining tests that depended on Jena (move to `jena-support/test` or use `mock`)
- Remove dependency in `Apache Jena` in `build.sbt`
- Run full test suite for both modules

✅ *Goal: `library` no longer has any dependency on Jena.*

---

## 🧪 Stage 6: Clean Up Tests

- Identify tests in `library/test` that use Jena types
- Either:
  - Move them to `jena-support/test`, **or**
  - Replace them with `sparql.mock` engine using `core.ext` traits
- Ensure `library/test` runs **without** Jena on the classpath

✅ *Goal: Pure-core test coverage without Jena.*

---

## 🚀 Stage 7: Update `app` to Use `jena-support`

- Add dependency: `app` → `jena-support`
- Remove any direct dependency on `library`
- Update imports to use `sparql.jena` via `jena-support`
- Validate application behavior end-to-end

✅ *Goal: `app` relies only on pluggable Jena engine.*

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
