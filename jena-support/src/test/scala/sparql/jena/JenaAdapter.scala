package sparql.jena

// Blocks:
//  B)  Apache Jena adapter to produce that algebra.
// -----------------------------------------------------------------------------
// **These file compiles with Spark 4.0.0 + Scala 2.13.14**

import org.apache.jena.query.Query
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.algebra.Algebra
import org.apache.jena.sparql.algebra.OpVisitorBase
import org.apache.jena.sparql.algebra.op._
import org.apache.jena.sparql.expr._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import sparql.core.ConstExpr
import sparql.core.NotEquals
import sparql.core.SBuilder
import sparql.core.TriplePattern
import sparql.core.VarExpr

import scala.jdk.CollectionConverters._

class JenaAdapter(b: SBuilder) extends OpVisitorBase {
  override def visit(opBGP: OpBGP): Unit = {
    val triples = opBGP.getPattern.getList.asScala.map { t =>
      TriplePattern(
        if (t.getSubject.isVariable) Right(t.getSubject.getName)
        else Left(t.getSubject.toString),
        if (t.getPredicate.isVariable) Right(t.getPredicate.getName)
        else Left(t.getPredicate.toString),
        if (t.getObject.isVariable) Right(t.getObject.getName)
        else Left(t.getObject.toString)
      )
    }.toSeq
    b.pushBgp(triples)
  }
  override def visit(opFilter: OpFilter): Unit = {
    val expr: Expr = opFilter.getExprs.get(0)
    expr match {
      case binExpr: ExprFunction2 =>
        val se = NotEquals(
          VarExpr(binExpr.getArg1.asVar().getVarName),
          ConstExpr(binExpr.getArg2.getConstant.asNode().getLiteralLexicalForm)
        )
        b.startFilter(se)
        opFilter.getSubOp.visit(this)
        b.endFilter()
      case other =>
        throw new UnsupportedOperationException(
          s"Unsupported filter expression: $other"
        )
    }
  }
  override def visit(opGraph: OpGraph): Unit = {
    val tgt =
      if (opGraph.getNode.isURI) Left(opGraph.getNode.toString)
      else Right(opGraph.getNode.getName)
    b.startGraph(tgt); opGraph.getSubOp.visit(this); b.endGraph()
  }
  override def visit(opProject: OpProject): Unit = {
    opProject.getSubOp.visit(this)
    b.setProject(opProject.getVars.asScala.map(_.getVarName).toSeq)
  }
}


