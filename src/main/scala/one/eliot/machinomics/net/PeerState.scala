package one.eliot.machinomics.net

sealed trait PeerState

object PeerState{
  case class Empty(network: Network, address: NetworkAddress) extends PeerState {
    def initial = Initial(network, address)
  }

  case class Initial(network: Network, address: NetworkAddress) extends PeerState {
    def connected(selfReportedAddress: NetworkAddress,
                  services: Services,
                  version: ProtocolVersion.Value,
                  userAgent: String,
                  height: Long) = Connected(network, address, selfReportedAddress, services, version, userAgent, height)
  }

  case class Connected(network: Network,
                       address: NetworkAddress,
                       selfReportedAddress: NetworkAddress,
                       services: Services,
                       version: ProtocolVersion.Value,
                       userAgent: String,
                       height: Long) extends PeerState {

    def acknowledged = Acknowledged(network, address, selfReportedAddress, services, version, userAgent, height)
  }

  case class Acknowledged(network: Network,
                          address: NetworkAddress,
                          selfReportedAddress: NetworkAddress,
                          services: Services,
                          version: ProtocolVersion.Value,
                          userAgent: String,
                          height: Long) extends PeerState
}
