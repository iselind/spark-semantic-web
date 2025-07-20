package sparql.core

import org.apache.spark.sql.catalyst.analysis.UnresolvedAttribute
import org.apache.spark.sql.catalyst.expressions.Alias
import org.apache.spark.sql.catalyst.expressions.EqualTo
import org.apache.spark.sql.catalyst.expressions.GreaterThan
import org.apache.spark.sql.catalyst.expressions.Literal
import org.apache.spark.sql.catalyst.expressions.SortOrder
import org.apache.spark.sql.catalyst.expressions.aggregate.Count
import org.apache.spark.sql.catalyst.plans.Inner
import org.apache.spark.sql.catalyst.plans.LeftOuter
import org.apache.spark.sql.catalyst.plans.logical.Aggregate
import org.apache.spark.sql.catalyst.plans.logical.Distinct
import org.apache.spark.sql.catalyst.plans.logical.JoinHint
import org.apache.spark.sql.catalyst.plans.logical.Limit
import org.apache.spark.sql.catalyst.plans.logical.LocalRelation
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.plans.logical.Project
import org.apache.spark.sql.catalyst.plans.logical.Sort

object SCompiler {
  def compile(qp: QueryPlan): LogicalPlan = {
    def compileWhere(op: SOp): LogicalPlan =
      op match {
        case Bgp(ts) => joinFromTriples(ts)
        case Filter(e, c) => {
          val child = compileWhere(c)
          org.apache.spark.sql.catalyst.plans.logical
            .Filter(exprToExpression(e, child), child)
        }
        case Join(l, r) =>
          org.apache.spark.sql.catalyst.plans.logical.Join(
            compileWhere(l),
            compileWhere(r),
            Inner,
            None,
            JoinHint.NONE
          )
        case LeftJoin(l, r) =>
          org.apache.spark.sql.catalyst.plans.logical.Join(
            compileWhere(l),
            compileWhere(r),
            LeftOuter,
            None,
            JoinHint.NONE
          )
        case Union(l, r) =>
          org.apache.spark.sql.catalyst.plans.logical.Union(
            compileWhere(l),
            compileWhere(r)
          )
        case Graph(Left(iri), p) => {
          org.apache.spark.sql.catalyst.plans.logical.Filter(
            EqualTo(
              UnresolvedAttribute("g"),
              Literal(iri)
            ),
            compileWhere(p)
          )
        }
        case Graph(Right(v), p) => {
          val child = compileWhere(p)
          val gAttr = UnresolvedAttribute("g")
          Project(Alias(gAttr, v)() +: child.output, child)
        }
        case ProjectVars(vs, c) => {
          val w = compileWhere(c)
          Project(
            vs.map(name => UnresolvedAttribute(name)),
            w
          )
        }
      }

    val where = compileWhere(qp.where)

    qp.form match {
      case s: Select    => compileSelect(where, s)
      case Ask          => compileAsk(where)
      case c: Construct => compileConstruct(where, c)
    }
  }

  private def joinFromTriples(
      ts: Seq[TriplePattern]
  ): LogicalPlan = {
    // Start with a placeholder LocalRelation (no schema).
    var newPlan: LogicalPlan = LocalRelation(Nil)

    // Apply filters for each constant term in the triple patterns.
    ts.foreach { t =>
      Seq(
        t.s.left.toOption.map("s" -> _),
        t.p.left.toOption.map("p" -> _),
        t.o.left.toOption.map("o" -> _)
      ).flatten.foreach { case (colN, v) =>
        newPlan = org.apache.spark.sql.catalyst.plans.logical.Filter(
          EqualTo(UnresolvedAttribute(colN), Literal(v)),
          newPlan
        )
      }
    }

    newPlan
  }

  private def exprToExpression(
      e: SExpr,
      plan: LogicalPlan
  ): org.apache.spark.sql.catalyst.expressions.Expression = e match {
    case VarExpr(n)   => UnresolvedAttribute(n)
    case ConstExpr(v) => Literal(v)
    case Equals(a, b) =>
      org.apache.spark.sql.catalyst.expressions
        .EqualTo(
          exprToExpression(a, plan),
          exprToExpression(b, plan)
        )
    case NotEquals(a, b) =>
      org.apache.spark.sql.catalyst.expressions.Not(
        org.apache.spark.sql.catalyst.expressions
          .EqualTo(
            exprToExpression(a, plan),
            exprToExpression(b, plan)
          )
      )
    // TODO: There are other operators as well, like <, >, and so on.
    // They are used for example to filter on numbers like age
  }

  private def compileSelect(base: LogicalPlan, s: Select): LogicalPlan = {
    val attrRefs = s.vars.map(n => UnresolvedAttribute(n))
    var lp: LogicalPlan =
      if (attrRefs.nonEmpty) Project(attrRefs, base) else base
    if (s.distinct) lp = Distinct(lp)
    if (s.orderBy.nonEmpty) {
      val sortExprs = s.orderBy.map { case (v, asc) =>
        val a =
          UnresolvedAttribute(
            v
          )
        SortOrder(
          a,
          if (asc) org.apache.spark.sql.catalyst.expressions.Ascending
          else org.apache.spark.sql.catalyst.expressions.Descending
        )
      }
      lp = Sort(sortExprs, global = true, lp)
    }
    if (s.limit.nonEmpty || s.offset > 0)
      lp = Limit(Literal(s.limit.getOrElse(Int.MaxValue)), lp)
    lp
  }

  private def compileAsk(base: LogicalPlan): LogicalPlan = {
    val cnt = Count(Literal(1))
    val gt0 = GreaterThan(cnt, Literal(0))
    val flag = Alias(gt0, "askResult")()
    val agg =
      Aggregate(Nil, Seq(flag), base) // produces a row with alias column
    Project(
      Seq(flag.toAttribute),
      agg
    ) // ensure column is a NamedExpression in Project
  }

  private def compileConstruct(base: LogicalPlan, c: Construct): LogicalPlan = {
    def litOrAttr(
        e: Either[String, String]
    ): org.apache.spark.sql.catalyst.expressions.Expression =
      e.fold(
        v => Literal(v),
        v => UnresolvedAttribute(v)
      )

    val rows = c.template.map { t =>
      val sExpr = litOrAttr(t.s); val pExpr = litOrAttr(t.p);
      val oExpr = litOrAttr(t.o)
      val proj = Project(
        Seq(Alias(sExpr, "s")(), Alias(pExpr, "p")(), Alias(oExpr, "o")()),
        base
      )
      proj
    }

    val unioned = rows.reduceLeft[LogicalPlan]((a, b) =>
      org.apache.spark.sql.catalyst.plans.logical.Union(a, b)
    )
    unioned
  }
}
