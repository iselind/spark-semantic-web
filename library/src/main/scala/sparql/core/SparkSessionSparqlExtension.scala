package sparql.core

import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.graphframes.GraphFrame
import sparql.core.context.SparQLContext
import sparql.core.exception.UnsupportedQuery
import sparql.core.query.Executor
import sparql.core.query.SelectNode

sealed trait SparqlResult {
  def asDataframe: DataFrame

  def asGraphFrame: GraphFrame

  def asBoolean: Boolean
}

case class SparqlDataFrame(df: DataFrame) extends SparqlResult {
  override def asDataframe: DataFrame = df

  override def asGraphFrame: GraphFrame = throw BadAccess("Not a GraphFrame")

  override def asBoolean: Boolean = throw BadAccess("Not a Boolean")
}

case class SparqlGraph(graph: GraphFrame) extends SparqlResult {
  override def asDataframe: DataFrame = throw BadAccess("Not a DataFrame")

  override def asGraphFrame: GraphFrame = graph

  override def asBoolean: Boolean = throw BadAccess("Not a Boolean")
}

case class SparqlBoolean(result: Boolean) extends SparqlResult {
  override def asDataframe: DataFrame = throw BadAccess("Not a DataFrame")

  override def asGraphFrame: GraphFrame = throw BadAccess("Not a GraphFrame")

  override def asBoolean: Boolean = result
}

trait SparqlQueryExecutor {
  def select(query: String): DataFrame

  def construct(query: String): GraphFrame

  def ask(query: String): Boolean
}

object SparkSessionSparqlExtension {
  implicit class SparqlEnhancedSession(spark: SparkSession)(implicit
      sparqlContext: SparQLContext
  ) {
    def sparql[T](query: String): SparqlResult = {
      val queryNode: Executor[T] = sparqlContext.sparqlParser.parse(query)
      sparql(queryNode)
    }

    private def sparql(s: SelectNode): SparqlResult = ???

    def registerGraph(frame: GraphFrame, name: String): Unit = {
      sparqlContext.graphStore.putGraph(name, frame)
    }

    def sparql(): SparqlQueryExecutor = new SparqlQueryExecutor {
      def select(query: String): DataFrame = {
        try {
          val queryNode = sparqlContext.sparqlParser.parse(query)
          sparql(queryNode) match {
            case SparqlDataFrame(f) => f
            case other => throw new IllegalArgumentException(s"Expected SELECT query but got: $other")
          }
        } catch {
          case _: UnsupportedQuery => throw new IllegalArgumentException("Query contains unsupported query elements")
        }
      }

      def construct(query: String): GraphFrame = {
        sparql(query) match {
          case SparqlGraph(g) => g
          case other => throw new IllegalArgumentException(s"Expected CONSTRUCT query but got: $other")
        }
      }

      def ask(query: String): Boolean = {
        sparql(query) match {
          case SparqlBoolean(b) => b
          case other => throw new IllegalArgumentException(s"Expected ASK query but got: $other")
        }
      }
    }
  }
}
