package sparql.core.executionstrategy

case class ParsingAborted(abortReason: Option[String]) extends Throwable {}
