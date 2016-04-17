package one.eliot.machinomics.net

import akka.actor.ActorRef
import one.eliot.machinomics.net.PeerState.GettingHeaders

object HerdState {

  object Factory {

    // TODO: remove hardcode
    def apply(prevState: HerdState, node: ActorRef, network: Network, mayBePeers: Set[ActorRef], connectedPeers: Set[ActorRef], gettingHeadersPeers: Set[ActorRef]): HerdState = {
      prevState match {
        case _: MayBe if connectedPeers.size >= 5 =>
          new Connected(node, network, mayBePeers, connectedPeers)

        case _: Connected if gettingHeadersPeers.nonEmpty =>
          new GettingHeaders(node, network, mayBePeers, connectedPeers, gettingHeadersPeers)

        case state => state
      }
    }

    def connect(state: HerdState, peer: ActorRef): HerdState = {
      Factory(state, state.node, state.network, state.mayBePeers - peer, state.connectedPeers + peer, state.gettingHeadersPeers)
    }

    def sendForHeaders(state: HerdState, peer: ActorRef): HerdState = {
      Factory(state, state.node, state.network, state.mayBePeers, state.connectedPeers - peer, state.gettingHeadersPeers + peer)
    }

    def finishGettingHeader(state: HerdState, peer: ActorRef): HerdState = {
      Factory(state, state.node, state.network, state.mayBePeers, state.connectedPeers + peer, state.gettingHeadersPeers - peer)
    }
  }

  sealed trait HerdState {
    def node: ActorRef

    def network: Network

    def mayBePeers: Set[ActorRef]
    def connectedPeers: Set[ActorRef]
    def gettingHeadersPeers: Set[ActorRef]
  }

  case class MayBe(node: ActorRef, network: Network, mayBePeers: Set[ActorRef], connectedPeers: Set[ActorRef] = Set.empty, gettingHeadersPeers: Set[ActorRef] = Set.empty) extends HerdState

  case class Connected(node: ActorRef, network: Network, mayBePeers: Set[ActorRef], connectedPeers: Set[ActorRef], gettingHeadersPeers: Set[ActorRef] = Set.empty) extends HerdState

  case class GettingHeaders(node: ActorRef, network: Network, mayBePeers: Set[ActorRef], connectedPeers: Set[ActorRef], gettingHeadersPeers: Set[ActorRef]) extends HerdState
}
