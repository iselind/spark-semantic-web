package sparql.core

import org.apache.spark.sql.DataFrame
import org.graphframes.GraphFrame

sealed trait SparqlResult {
  protected def badAccess(name: String): Nothing =
    throw BadAccess(s"Not a $name result")

  def asDataframe: DataFrame = badAccess("DataFrame")
  def asGraphFrame: GraphFrame = badAccess("GraphFrame")
  def asBoolean: Boolean = badAccess("Boolean")
}

case class SparqlDataFrame(df: DataFrame) extends SparqlResult {
  override def asDataframe: DataFrame = df
}
object SparqlDataFrame {
  implicit def toDataFrame(sd: SparqlDataFrame): DataFrame = sd.df
}

case class SparqlGraph(graph: GraphFrame) extends SparqlResult {
  override def asGraphFrame: GraphFrame = graph
}
object SparqlGraph {
  implicit def toGraphFrame(sg: SparqlGraph): GraphFrame = sg.graph
}

case class SparqlBoolean(result: Boolean) extends SparqlResult {
  override def asBoolean: Boolean = result
}
object SparqlBoolean {
  implicit def toBoolean(b: SparqlBoolean): Boolean = b.result
}
