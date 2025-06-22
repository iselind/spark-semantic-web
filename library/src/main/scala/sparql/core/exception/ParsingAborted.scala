package sparql.core.exception

case class ParsingAborted(abortReason: Option[String]) extends Throwable {}
