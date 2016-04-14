package one.eliot.machinomics.net

trait Herd {

}

object Herd {
  sealed trait Message
  case class Start() extends Message
  case class Stop() extends Message
}
