package one.eliot.machinomics.net

import akka.actor.ActorRef

object PeerState{
  sealed trait PeerState{
    def herd: ActorRef
  }

  case class Initial(network: Network, address: NetworkAddress, herd: ActorRef) extends PeerState {
    def connected(wire: ActorRef) = Connected(network, address, herd, wire)
  }

  sealed trait Wired {
    def wire: ActorRef
  }

  case class Connected(network: Network, address: NetworkAddress, herd: ActorRef, wire: ActorRef) extends PeerState with Wired {
    def registered(selfReportedAddress: NetworkAddress,
                   services: Services,
                   version: ProtocolVersion.Value,
                   userAgent: String,
                   height: Long) = Registered(network, address, selfReportedAddress, services, version, userAgent, height, herd, wire)
  }

  case class Registered(network: Network,
                        address: NetworkAddress,
                        selfReportedAddress: NetworkAddress,
                        services: Services,
                        version: ProtocolVersion.Value,
                        userAgent: String,
                        height: Long,
                        herd: ActorRef,
                        wire: ActorRef) extends PeerState with Wired {

    def acknowledged = Acknowledged(network, address, selfReportedAddress, services, version, userAgent, height, herd, wire)
  }

  case class Acknowledged(network: Network,
                          address: NetworkAddress,
                          selfReportedAddress: NetworkAddress,
                          services: Services,
                          version: ProtocolVersion.Value,
                          userAgent: String,
                          height: Long,
                          herd: ActorRef,
                          wire: ActorRef) extends PeerState with Wired {

    def gettingHeaders = GettingHeaders(network, address, selfReportedAddress, services, version, userAgent, height, 0, herd, wire)

  }

  case class GettingHeaders(network: Network,
                            address: NetworkAddress,
                            selfReportedAddress: NetworkAddress,
                            services: Services,
                            version: ProtocolVersion.Value,
                            userAgent: String,
                            height: Long,
                            downloadedHeaderCount: Int,
                            herd: ActorRef,
                            wire: ActorRef) extends PeerState with Wired {

    def acknowledged = Acknowledged(network, address, selfReportedAddress, services, version, userAgent, height, herd, wire)

    def next(addCount: Int): GettingHeaders = GettingHeaders(network, address, selfReportedAddress, services, version, userAgent, height, downloadedHeaderCount + addCount, herd, wire)
  }
}
