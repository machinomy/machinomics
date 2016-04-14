package one.eliot.machinomics.net

import one.eliot.machinomics.net.protocol._

sealed trait PeerState

case class EmptyPeerState(network: Network, address: NetworkAddress) extends PeerState
