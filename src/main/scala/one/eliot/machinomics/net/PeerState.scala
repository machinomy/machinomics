package one.eliot.machinomics.net

import akka.actor.ActorRef

object PeerState{
  sealed trait PeerState{
    def herd: ActorRef
  }

  case class Initial(network: Network, address: NetworkAddress, herd: ActorRef) extends PeerState {
    def connected = Connected(network, address, herd)
  }

  case class Connected(network: Network, address: NetworkAddress, herd: ActorRef) extends PeerState {
    def registered(selfReportedAddress: NetworkAddress,
                   services: Services,
                   version: ProtocolVersion.Value,
                   userAgent: String,
                   height: Long) = Registered(network, address, selfReportedAddress, services, version, userAgent, height, herd)
  }

  case class Registered(network: Network,
                        address: NetworkAddress,
                        selfReportedAddress: NetworkAddress,
                        services: Services,
                        version: ProtocolVersion.Value,
                        userAgent: String,
                        height: Long,
                        herd: ActorRef) extends PeerState {

    def acknowledged = Acknowledged(network, address, selfReportedAddress, services, version, userAgent, height, herd)
  }

  case class Acknowledged(network: Network,
                          address: NetworkAddress,
                          selfReportedAddress: NetworkAddress,
                          services: Services,
                          version: ProtocolVersion.Value,
                          userAgent: String,
                          height: Long,
                          herd: ActorRef) extends PeerState
}
