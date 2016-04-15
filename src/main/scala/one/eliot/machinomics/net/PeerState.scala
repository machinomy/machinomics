package one.eliot.machinomics.net

object PeerState{
  sealed trait PeerState

  case class Empty(network: Network, address: NetworkAddress) extends PeerState {
    def connected = Connected(network, address)
  }

  case class Connected(network: Network, address: NetworkAddress) extends PeerState {
    def registered(selfReportedAddress: NetworkAddress,
                   services: Services,
                   version: ProtocolVersion.Value,
                   userAgent: String,
                   height: Long) = Registered(network, address, selfReportedAddress, services, version, userAgent, height)
  }

  case class Registered(network: Network,
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
