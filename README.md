# Spark Semantic Web

This project is split into three pieces:

- A library with the core functionality.
- Jena specific library providing Jena based implementations of things the library is missing but require to work.
- An example application using the Jena library to exemplify usage

The intention of this project is to explore the possibility of using Apache
Spark's query optimizations to speed up SparkQL queries.

## The App

Shows example usage of the library's `sparkql()` implementation.

You run it by executing `sbt app/run`.

## The Library

Introduces a `sparql()` to the Apache Spark context.

The implementation of `sparql()` takes a sparql query and runs it. How that execution works is up to the implicit
execution strategy available.

THe end goal is that Apache Jena will never have to be used to execute SparQL.
Instead, it will be executed entirely self-sufficiently by Apache Spark. For simplicity we will use something
implementing `sparql.core.ext.SparqlParser` to properly dissect the query and figure out if we can transform the query
into something Apache Spark can execute.

Beyond the `sparql()` addition we have also added the possibility to register graph frames with associated names. The
intention is that these names will later be referenced in the Sparql queries, similarlly to how Apache Spark supports
registering table views.

## The API

The high-level API provided looks like this:

```scala
def registerGraph(frame: GraphFrame, name: String)

def sparql(query: String): DataFrame = {
  strategy.execute(query, graphStore)(spark)
}
```

and can be found in `SparkSessionSparqlExtension.scala`
