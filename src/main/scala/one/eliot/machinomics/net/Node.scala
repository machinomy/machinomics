package one.eliot.machinomics.net

trait Node {

}

object Node {
  sealed trait Message
  case class Start() extends Message
  case class Stop() extends Message
}
