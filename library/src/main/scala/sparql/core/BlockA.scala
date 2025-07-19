package sparql.core

// -----------------------------------------------------------------------------
// BLOCK A  -  algebra   +   builder   +   Spark compiler
// -----------------------------------------------------------------------------


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
case object Ask extends ResultForm  // Shouldn't this be a case class?
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