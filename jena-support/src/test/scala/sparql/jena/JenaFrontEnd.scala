package sparql.jena

import org.apache.jena.query.Query
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.algebra.Algebra
import sparql.core.Ask
import sparql.core.Select
import sparql.core.StackSBuilder

import scala.jdk.CollectionConverters._

object JenaFrontEnd {
  def compile(q: String): StackSBuilder = {
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
    sb
  }
}
