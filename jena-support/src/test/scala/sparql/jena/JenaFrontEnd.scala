package sparql.jena

import org.apache.jena.query.Query
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.algebra.Algebra
import org.apache.jena.sparql.algebra.OpVisitorBase
import org.apache.jena.sparql.algebra.op._
import org.apache.jena.sparql.expr._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import sparql.core.Ask
import sparql.core.SCompiler
import sparql.core.Select
import sparql.core.StackSBuilder

import scala.jdk.CollectionConverters._

object JenaFrontEnd {
  def compile(q: String, tableName: String)(implicit
      spark: SparkSession
  ): LogicalPlan = {
    compile(q, spark.table(tableName))
  }
  def compile(q: String, df: DataFrame)(implicit
      spark: SparkSession
  ): LogicalPlan = {
    compile(q, df.queryExecution.analyzed) // or `.logical`?
  }
  def compile(q: String, lp: LogicalPlan)(implicit
      spark: SparkSession
  ): LogicalPlan = {
    val query = QueryFactory.create(q)
    val algebra = Algebra.compile(query)
    val sb = new StackSBuilder

    query.getQueryType match {
      case Query.QueryTypeSelect =>
        sb.setResultForm(
          Select(
            query.getProjectVars.asScala.map(_.getVarName).toSeq,
            query.isDistinct
          )
        )
      case Query.QueryTypeAsk => sb.setResultForm(Ask)
      case _ =>
        throw new UnsupportedOperationException("only SELECT/ASK in skeleton")
    }

    algebra.visit(new JenaAdapter(sb))
    SCompiler.compile(sb.result(), lp)
  }
}
