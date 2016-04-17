package one.eliot.machinomics.net

import akka.actor.ActorRef

object HerdState {
  sealed trait HerdState
  case class Connected(network: Network, peers: Seq[ActorRef]) extends HerdState
}
