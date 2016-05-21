package one.eliot.machinomics.net

import akka.actor.ActorRef

object HerdStateA {

  case class Status(status: String)

  val EMPTY = Status("empty")
  val CONNECTING = Status("connecting")
  val CONNECTED = Status("connected")
  val GETTING_HEADERS = Status("getting_headers")

  def connect(state: HerdStateA, peer: ActorRef): HerdStateA = {
    val newMayBePeers = state.mayBePeers - peer
    val newConnectedPeers = state.connectedPeers + peer
    val newStatus = if (newConnectedPeers.size >= 5 && state.gettingHeadersPeers.isEmpty) CONNECTED else state.status
    new HerdStateA(state.node, state.network, newStatus, newMayBePeers, newConnectedPeers, state.gettingHeadersPeers)
  }

  def sendForHeaders(state: HerdStateA, peer: ActorRef): HerdStateA = {
    val newConnectedPeers = state.connectedPeers - peer
    val newGettingHeadersPeers = state.gettingHeadersPeers + peer
    val newStatus = if (newGettingHeadersPeers.nonEmpty) GETTING_HEADERS else state.status
    new HerdStateA(state.node, state.network, newStatus, state.mayBePeers, newConnectedPeers, newGettingHeadersPeers)
  }

  def finishGettingHeaders(state: HerdStateA, peer: ActorRef): HerdStateA = {
    val newConnectedPeers = state.connectedPeers + peer
    val newGettingHeadersPeers = state.gettingHeadersPeers - peer
    val newStatus = if (newGettingHeadersPeers.isEmpty) CONNECTED else state.status
    new HerdStateA(state.node, state.network, newStatus, state.mayBePeers, newConnectedPeers, newGettingHeadersPeers)
  }
}

case class HerdStateA(node: ActorRef,
                      network: Network,
                      status: HerdStateA.Status,
                      mayBePeers: Set[ActorRef],
                      connectedPeers: Set[ActorRef] = Set.empty,
                      gettingHeadersPeers: Set[ActorRef] = Set.empty)
