package sparql.core.ext

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.graphframes.GraphFrame

sealed trait ParsedQuery { self =>
  type Out
  def exec()(implicit spark: SparkSession): Out
}

final case class Select() extends ParsedQuery {
  type Out = DataFrame
  def exec()(implicit spark: SparkSession): DataFrame = ???
}

final case class Construct() extends ParsedQuery {
  type Out = GraphFrame
  def exec()(implicit spark: SparkSession): GraphFrame = ???
}

final case class Ask() extends ParsedQuery {
  type Out = Boolean
  def exec()(implicit spark: SparkSession): Boolean = ???
}

// Temporary beast, only here until we can represent a complete query regardless of kind
final case class Unsupported(q: String) extends ParsedQuery {
  type Out = Unit
  def exec()(implicit spark: SparkSession): Unit = println(q)
}

trait SparqlParser {
  def parse(query: String): ParsedQuery
}
