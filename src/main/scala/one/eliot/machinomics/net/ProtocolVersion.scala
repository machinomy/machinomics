package one.eliot.machinomics.net

object ProtocolVersion {
  case class Value(number: Int)
  val MINIMUM = Value(70000)
  val PONG = Value(60001)
  val BLOOM_FILTER = Value(70000)
  val CURRENT = Value(70001)
}
