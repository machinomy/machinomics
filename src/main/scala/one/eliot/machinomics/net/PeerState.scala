package one.eliot.machinomics.net

import one.eliot.machinomics.net.protocol._

sealed trait PeerState

case class InitialPeerState(network: Network, address: NetworkAddress) extends PeerState

case class ConnectedPeerState(network: Network,
                              address: NetworkAddress,
                              selfReportedAddress: NetworkAddress,
                              services: Services,
                              version: ProtocolVersion.Value,
                              userAgent: String,
                              height: Long,
                              acked: Boolean) extends PeerState
