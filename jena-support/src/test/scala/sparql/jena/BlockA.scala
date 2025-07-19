package sparql.jena

// -----------------------------------------------------------------------------
// BLOCK A  -  algebra   +   builder   +   Spark compiler
// -----------------------------------------------------------------------------
import org.apache.jena.sparql.expr.Expr
import org.apache.jena.sparql.expr.ExprFunction2
import org.apache.spark.sql.Column
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.expressions.Alias
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.catalyst.expressions.EqualTo
import org.apache.spark.sql.catalyst.expressions.GreaterThan
import org.apache.spark.sql.catalyst.expressions.Literal
import org.apache.spark.sql.catalyst.expressions.SortOrder
import org.apache.spark.sql.catalyst.expressions.aggregate.Count
import org.apache.spark.sql.catalyst.plans.Inner
import org.apache.spark.sql.catalyst.plans.JoinType
import org.apache.spark.sql.catalyst.plans.LeftOuter
import org.apache.spark.sql.catalyst.plans.logical.Aggregate
import org.apache.spark.sql.catalyst.plans.logical.Distinct
import org.apache.spark.sql.catalyst.plans.logical.JoinHint
import org.apache.spark.sql.catalyst.plans.logical.Limit
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.plans.logical.Project
import org.apache.spark.sql.catalyst.plans.logical.Sort
import org.apache.spark.sql.functions.col
import org.apache.spark.sql.functions.lit

// -- 1 · Algebra --------------------------------------------------------------
sealed trait SOp
case class Bgp(triples: Seq[TriplePattern]) extends SOp
case class Filter(expr: SExpr, child: SOp) extends SOp
case class Join(left: SOp, right: SOp) extends SOp
case class LeftJoin(left: SOp, right: SOp)
    extends SOp // TODO: This should really be called "Optional", that is the Sparql term. We'll interpret that as a left join but that is not Jena's business
case class Union(left: SOp, right: SOp) extends SOp
case class Graph(target: Either[String, String], pattern: SOp) extends SOp
case class ProjectVars(vars: Seq[String], child: SOp) extends SOp

// -- 2 · Expressions ----------------------------------------------------------
sealed trait SExpr
case class VarExpr(name: String) extends SExpr
case class ConstExpr(value: String) extends SExpr
case class NotEquals(a: SExpr, b: SExpr) extends SExpr

case class TriplePattern(
    s: Either[String, String],
    p: Either[String, String],
    o: Either[String, String]
)

// -- 3 · Result form ----------------------------------------------------------
sealed trait ResultForm
case class Select(
    vars: Seq[String],
    distinct: Boolean = false,
    orderBy: Seq[(String, Boolean)] = Nil,
    limit: Option[Int] = None,
    offset: Int = 0
) extends ResultForm
case object Ask extends ResultForm
case class Construct(template: Seq[TriplePattern]) extends ResultForm

case class QueryPlan(where: SOp, form: ResultForm)

// -- 4 · Builder interface ----------------------------------------------------
trait SBuilder {
  def pushBgp(triples: Seq[TriplePattern]): Unit
  def startFilter(expr: SExpr): Unit; def endFilter(): Unit
  def startOptional(): Unit; def endOptional(): Unit
  def startUnion(): Unit; def endUnion(): Unit
  def startGraph(target: Either[String, String]): Unit; def endGraph(): Unit
  def setProject(vars: Seq[String]): Unit
  def setResultForm(form: ResultForm): Unit
  def result(): QueryPlan
}

// -- 5 · Minimal stack‑based Builder -----------------------------------------
import scala.collection.mutable.Stack
class StackSBuilder extends SBuilder {
  private val nodes = Stack[SOp]()
  private val closings = Stack[() => Unit]()
  private var qForm: ResultForm = Select(Nil)

  override def pushBgp(t: Seq[TriplePattern]): Unit = nodes.push(Bgp(t))

  override def startFilter(e: SExpr): Unit =
    closings.push(() => nodes.push(Filter(e, nodes.pop())))
  override def endFilter(): Unit = closings.pop()()

  override def startOptional(): Unit =
    closings.push(() => {
      val r = nodes.pop(); val l = nodes.pop(); nodes.push(LeftJoin(l, r))
    })
  override def endOptional(): Unit = closings.pop()()

  override def startUnion(): Unit =
    closings.push(() => {
      val r = nodes.pop(); val l = nodes.pop(); nodes.push(Union(l, r))
    })
  override def endUnion(): Unit = closings.pop()()

  override def startGraph(t: Either[String, String]): Unit =
    closings.push(() => nodes.push(Graph(t, nodes.pop())))
  override def endGraph(): Unit = closings.pop()()

  override def setProject(vs: Seq[String]): Unit =
    nodes.push(ProjectVars(vs, nodes.pop()))
  override def setResultForm(f: ResultForm): Unit = qForm = f
  override def result(): QueryPlan = QueryPlan(nodes.pop(), qForm)
}

// -- 6 · Compiler  (SOp → LogicalPlan) ---------------------------------------
object SCompiler {
  def compile(qp: QueryPlan, startPlan: LogicalPlan)(implicit
      spark: SparkSession
  ): LogicalPlan = {
    def compileWhere(op: SOp)(implicit spark: SparkSession): LogicalPlan =
      op match {
        case Bgp(ts) => joinFromTriples(ts, startPlan)
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
          val w = compileWhere(p)
          org.apache.spark.sql.catalyst.plans.logical.Filter(
            EqualTo(
              getAttribute(w, "g"),
              Literal(iri)
            ),
            compileWhere(p)
          )
        }
        case Graph(Right(v), p) => {
          val child = compileWhere(p)
          val gAttr = getAttribute(child, "g")
          Project(Alias(gAttr, v)() +: child.output, child)
        }
        case ProjectVars(vs, c) => {
          val w = compileWhere(c)
          Project(
            vs.map(name => getAttribute(w, name)),
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
      ts: Seq[TriplePattern],
      plan: LogicalPlan
  )(implicit spark: SparkSession): LogicalPlan = {
    // helper to build AttributeReference for given column name once
    def attr(name: String) = getAttribute(plan, name)
    var newPlan = plan
    ts.foreach { t =>
      Seq(
        t.s.left.toOption.map("s" -> _),
        t.p.left.toOption.map("p" -> _),
        t.o.left.toOption.map("o" -> _)
      ).flatten.foreach { case (colN, v) =>
        newPlan = org.apache.spark.sql.catalyst.plans.logical
          .Filter(EqualTo(attr(colN), Literal(v)), newPlan)
      }
    }
    newPlan
  }

  private def exprToCol(e: SExpr): Column = e match {
    case VarExpr(n)      => col(n)
    case ConstExpr(v)    => lit(v)
    case NotEquals(a, b) => exprToCol(a) =!= exprToCol(b)
  }

  private def exprToExpression(
      e: SExpr,
      plan: LogicalPlan
  ): org.apache.spark.sql.catalyst.expressions.Expression = e match {
    case VarExpr(n)   => getAttribute(plan, n)
    case ConstExpr(v) => Literal(v)
    case NotEquals(a, b) =>
      org.apache.spark.sql.catalyst.expressions.Not(
        org.apache.spark.sql.catalyst.expressions
          .EqualTo(
            exprToExpression(a, plan),
            exprToExpression(b, plan)
          )
      )
  }

  private def getAttribute(lp: LogicalPlan, name: String): Attribute = {
    val inputAttrs = lp.output
    inputAttrs
      .find(_.name == name)
      .getOrElse {
        sys.error(
          s"Attribute $name not found in child schema. " +
            s"Available: ${inputAttrs.map(_.name).mkString("[", ", ", "]")}"
        )
      }
  }

  private def compileSelect(base: LogicalPlan, s: Select): LogicalPlan = {
    val attrRefs = s.vars.map(n => getAttribute(base, n))
    var lp: LogicalPlan =
      if (attrRefs.nonEmpty) Project(attrRefs, base) else base
    if (s.distinct) lp = Distinct(lp)
    if (s.orderBy.nonEmpty) {
      val sortExprs = s.orderBy.map { case (v, asc) =>
        val a =
          getAttribute(lp, v) // Might be that lp here should be base, not sure
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
        v => getAttribute(base, v)
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
