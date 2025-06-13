package sparql.executionstrategy

case class ParsingAborted(abortReason: Option[String]) extends Throwable {}
