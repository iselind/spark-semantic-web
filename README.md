# Spark Semantic Web

This project is split into a library and an example application using that library.

The intention of this project is to explore the possibility of using Apache
Spark's query optimizations to speed up SparkQL queries.

## The App

Shows example usage of the library's `sparkql()` implementation.

You run it by executing `sbt app/run`.

## The Library

Introduces a `sparql()` to the Apache Spark context.

The implementation of `sparql()` takes a sparql query and runs it through
Apache Spark if it doesn't contain, currently, unsupported features.
Otherwise
the execution is handled by Apache Jena.

THe end goal is that Apache Jena will never have to be used to execute SparQL.
Instead it will be executed entirely self-sufficiently by Apache Spark.
