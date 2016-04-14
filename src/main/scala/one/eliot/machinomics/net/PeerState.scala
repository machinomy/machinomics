package one.eliot.machinomics.net

sealed trait PeerState

object PeerState{
  case class Initial(network: Network, address: NetworkAddress) extends PeerState

  case class Connected(network: Network,
                       address: NetworkAddress,
                       selfReportedAddress: NetworkAddress,
                       services: Services,
                       version: ProtocolVersion.Value,
                       userAgent: String,
                       height: Long) extends PeerState

  case class Acked(network: Network,
                   address: NetworkAddress,
                   selfReportedAddress: NetworkAddress,
                   services: Services,
                   version: ProtocolVersion.Value,
                   userAgent: String,
                   height: Long) extends PeerState
}
