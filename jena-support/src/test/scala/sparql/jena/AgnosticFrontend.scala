package sparql.jena

// -----------------------------------------------------------------------------
// sparql_spark_scaffold.scala  --  minimal end-to-end prototype
// -----------------------------------------------------------------------------
// • Block A  -  Library-agnostic SPARQL algebra + Builder + compiler to Catalyst
// • Block B  -  Thin adapter from Apache Jena AST to the Builder
// • Block C  -  Tiny in-memory tests with MUnit (or ScalaTest) proving correctness
// -----------------------------------------------------------------------------

// -----------------------------------------------------------------------------
// BLOCK A  -  algebra   +   builder   +   Spark compiler
// -----------------------------------------------------------------------------
import org.apache.spark.sql.Column
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.plans.logical._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.catalyst.expressions._

// --- 1 · Algebra ----------------------------------------------------------------
sealed trait SOp
case class Bgp(triples: Seq[TriplePattern]) extends SOp
case class Filter(expr: SExpr, child: SOp) extends SOp
case class Join(left: SOp, right: SOp) extends SOp
case class LeftJoin(left: SOp, right: SOp) extends SOp
case class Union(left: SOp, right: SOp) extends SOp
case class Graph(target: Either[String, String], pattern: SOp) extends SOp
case class ProjectVars(vars: Seq[String], child: SOp) extends SOp

// --- 2 · Expressions -------------------------------------------------------------
sealed trait SExpr
case class VarExpr(name: String) extends SExpr
case class ConstExpr(value: String) extends SExpr
case class NotEquals(a: SExpr, b: SExpr) extends SExpr
// (extend as needed)

case class TriplePattern(
    s: Either[String, String],
    p: Either[String, String],
    o: Either[String, String]
)

// --- 3 · ResultForm + QueryPlan --------------------------------------------------
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

// --- 4 · Functional builder interface (visitor target) ---------------------------
trait SBuilder {
  def pushBgp(triples: Seq[TriplePattern]): Unit
  def startFilter(expr: SExpr): Unit
  def endFilter(): Unit
  def startLeftJoin(): Unit
  def endLeftJoin(): Unit
  def startUnion(): Unit
  def endUnion(): Unit
  def startGraph(target: Either[String, String]): Unit
  def endGraph(): Unit
  def setProject(vars: Seq[String]): Unit
  def setResultForm(form: ResultForm): Unit
  def result(): QueryPlan
}

// --- 5 · Simple stack‑based implementation --------------------------------------
class StackSBuilder extends SBuilder {
  private val nodeStack = new scala.collection.mutable.Stack[SOp]
  private val opStack = new scala.collection.mutable.Stack[() => Unit]
  private var resultForm: ResultForm = Select(Nil)

  override def pushBgp(ts: Seq[TriplePattern]): Unit =
    nodeStack.push(Bgp(ts))

  override def startFilter(expr: SExpr): Unit =
    opStack.push(() => {
      val child = nodeStack.pop()
      nodeStack.push(Filter(expr, child))
    })
  override def endFilter(): Unit = opStack.pop()()

  override def startLeftJoin(): Unit =
    opStack.push(() => {
      val right = nodeStack.pop(); val left = nodeStack.pop()
      nodeStack.push(LeftJoin(left, right))
    })
  override def endLeftJoin(): Unit = opStack.pop()()

  override def startUnion(): Unit =
    opStack.push(() => {
      val right = nodeStack.pop(); val left = nodeStack.pop()
      nodeStack.push(Union(left, right))
    })
  override def endUnion(): Unit = opStack.pop()()

  override def startGraph(t: Either[String, String]): Unit =
    opStack.push(() => {
      val child = nodeStack.pop(); nodeStack.push(Graph(t, child))
    })
  override def endGraph(): Unit = opStack.pop()()

  override def setProject(vars: Seq[String]): Unit =
    nodeStack.push(ProjectVars(vars, nodeStack.pop()))

  override def setResultForm(form: ResultForm): Unit = resultForm = form

  override def result(): QueryPlan = QueryPlan(nodeStack.pop(), resultForm)
}

// --- 6 · Compiler  SOp  →  Catalyst LogicalPlan ----------------------------------
object SCompiler {
  def compile(qp: QueryPlan)(implicit spark: SparkSession): LogicalPlan = {
    val whereLP = compileWhere(qp.where)
    qp.form match {
      case s: Select    => compileSelect(whereLP, s)
      case Ask          => compileAsk(whereLP)
      case c: Construct => compileConstruct(whereLP, c)
    }
  }

  private def compileWhere(op: SOp)(implicit spark: SparkSession): LogicalPlan =
    op match {
      case Bgp(triples) => buildJoin(triples)
      case Filter(expr, child) =>
        Filter(exprToColumn(expr).expr, compileWhere(child))
      case Join(l, r) =>
        Join(compileWhere(l), compileWhere(r), JoinType("Inner"), None)
      case LeftJoin(l, r) =>
        Join(compileWhere(l), compileWhere(r), JoinType("LeftOuter"), None)
      case Union(l, r) => Union(compileWhere(l), compileWhere(r))
      case Graph(Either.Left(iri), pattern) =>
        val child = compileWhere(pattern)
        Filter(EqualTo(Symbol("g"), Literal(iri)), child)
      case Graph(Either.Right(varName), pattern) =>
        val child = compileWhere(pattern)
        Project(Symbol("g").as(varName) +: child.output, child)
      case ProjectVars(vars, child) =>
        Project(vars.map(Symbol(_)), compileWhere(child))
    }

  private def buildJoin(
      triples: Seq[TriplePattern]
  )(implicit spark: SparkSession): LogicalPlan = {
    var plan: LogicalPlan = spark.table("quads").logicalPlan
    triples.foreach { t =>
      val sFilter = t.s.left.toOption.map(v => EqualTo(Symbol("s"), Literal(v)))
      val pFilter = t.p.left.toOption.map(v => EqualTo(Symbol("p"), Literal(v)))
      val oFilter = t.o.left.toOption.map(v => EqualTo(Symbol("o"), Literal(v)))
      val cond = Seq(sFilter, pFilter, oFilter).flatten.reduceOption(And(_, _))
      plan = cond.map(Filter(_, plan)).getOrElse(plan)
    }
    plan
  }

  private def exprToColumn(e: SExpr): Column = e match {
    case VarExpr(n)      => col(n)
    case ConstExpr(v)    => lit(v)
    case NotEquals(a, b) => exprToColumn(a) =!= exprToColumn(b)
  }

  private def compileSelect(lp: LogicalPlan, s: Select): LogicalPlan = {
    var out = lp
    if (s.vars.nonEmpty) out = Project(s.vars.map(Symbol(_)), out)
    if (s.distinct) out = Distinct(out)
    if (s.orderBy.nonEmpty)
      out = Sort(
        s.orderBy.map { case (v, asc) =>
          SortOrder(Symbol(v), if (asc) Ascending else Descending)
        },
        true,
        out
      )
    if (s.limit.nonEmpty || s.offset > 0)
      out = Limit(Literal(s.limit.getOrElse(Int.MaxValue)), out)
    out
  }

  private def compileAsk(lp: LogicalPlan): LogicalPlan = {
    val exists =
      Alias(GreaterThan(Count(Literal(1)), Literal(0)), "askResult")()
    Project(Seq(exists), Aggregate(Nil, Seq(exists.child), lp))
  }

  private def compileConstruct(lp: LogicalPlan, c: Construct): LogicalPlan = {
    val rows = c.template.map { t =>
      val sCol = t.s.fold(lit, col)
      val pCol = t.p.fold(lit, col)
      val oCol = t.o.fold(lit, col)
      Project(Seq(sCol.as("s"), pCol.as("p"), oCol.as("o")), lp)
    }
    rows.reduce(Union(_, _))
  }
}

// -----------------------------------------------------------------------------
// BLOCK B  –  Thin Apache Jena adapter → StackSBuilder
// -----------------------------------------------------------------------------
import org.apache.jena.query.{Query, QueryFactory}
import org.apache.jena.sparql.algebra.{Algebra, OpVisitorBase}
import org.apache.jena.sparql.algebra.op._
import scala.jdk.CollectionConverters._

class JenaAdapter(builder: SBuilder) extends OpVisitorBase {
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
    }
    builder.pushBgp(triples.toSeq)
  }
  override def visit(opFilter: OpFilter): Unit = {
    // Simplified: assume single != filter like (?o != "z") for demo
    val expr = opFilter.getExprs.get(0)
    val sExpr = NotEquals(
      VarExpr(expr.getArg1.asVar().getVarName),
      ConstExpr(expr.getArg2.getConstant.getLexicalForm)
    )
    builder.startFilter(sExpr)
    opFilter.getSubOp.visit(this)
    builder.endFilter()
  }
  override def visit(opGraph: OpGraph): Unit = {
    val target =
      if (opGraph.getNode.isURI) Left(opGraph.getNode.toString)
      else Right(opGraph.getNode.getName)
    builder.startGraph(target)
    opGraph.getSubOp.visit(this)
    builder.endGraph()
  }
  override def visit(opProject: OpProject): Unit = {
    builder.startFilter(
      ConstExpr("true")
    ) // push a dummy frame to keep stack aligned
    opProject.getSubOp.visit(this)
    builder.endFilter()
    builder.setProject(opProject.getVars.asScala.map(_.getVarName))
  }
  // implement other visit methods as needed…
}

object JenaFrontEnd {
  def compile(queryStr: String)(implicit spark: SparkSession): LogicalPlan = {
    val q: Query = QueryFactory.create(queryStr)
    val algebra = Algebra.compile(q)
    val sb = new StackSBuilder
    // result form
    q.getQueryType match {
      case Query.QueryTypeSelect =>
        sb.setResultForm(Select(q.getProjectVars.asScala.map(_.getVarName)))
      case Query.QueryTypeAsk => sb.setResultForm(Ask)
      // Construct omitted for brevity
    }
    algebra.visit(new JenaAdapter(sb))
    val qp = sb.result()
    SCompiler.compile(qp)
  }
}

// -----------------------------------------------------------------------------
// BLOCK C  –  Mini unit tests (MUnit) proving correctness
// -----------------------------------------------------------------------------
import munit.FunSuite
import org.apache.jena.query.{QueryExecutionFactory, DatasetFactory}

class SparqlSparkSpec extends FunSuite {
  implicit val spark: SparkSession =
    SparkSession.builder.master("local[1]").appName("test").getOrCreate()
  import spark.implicits._

  // tiny in‑memory quad view
  Seq(
    ("", "s1", "p", "o"),
    ("http://g1", "x", "p", "y"),
    ("http://g1", "y", "p", "z")
  )
    .toDF("g", "s", "p", "o")
    .createOrReplaceTempView("quads")

  // same data into Jena dataset
  private val ds = DatasetFactory.create()
  ds.getDefaultModel.read("data:,<s1> <p> <o> .")
  ds.addNamedModel(
    "http://g1",
    ds.getDefaultModel.read("data:,<x> <p> <y> . <y> <p> <z> .")
  )

  test("ASK with GRAPH clause") {
    val q =
      """ASK WHERE { GRAPH <http://g1> { ?s <p> ?o . FILTER(?o != \"z\") } }"""
    val sparkRes = JenaFrontEnd.compile(q).executeCollect().head.getBoolean(0)
    val jenaRes = QueryExecutionFactory.create(q, ds).execAsk()
    assertEquals(sparkRes, jenaRes)
  }

  test("SELECT basic BGP") {
    val q = "SELECT ?s WHERE { ?s <p> <o> }"
    val sparkRes =
      JenaFrontEnd.compile(q).executeCollect().map(_.getString(0)).toSet
    val jenaRes = QueryExecutionFactory
      .create(q, ds)
      .execSelect()
      .asScala
      .map(_.get("s").toString)
      .toSet
    assertEquals(sparkRes, jenaRes)
  }
}
