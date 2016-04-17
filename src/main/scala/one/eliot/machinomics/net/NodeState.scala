package one.eliot.machinomics.net

import java.net.InetAddress

import akka.actor.ActorRef

object NodeState {
  sealed trait NodeState
  case class Initial(network: Network,
                     address: NetworkAddress,
                     services: Services,
                     userAgent: String,
                     relayBeforeFilter: Boolean,
                     peersCount: Int) extends NodeState {
    def working(herd: ActorRef) = Working(network, address, services, userAgent, relayBeforeFilter, herd)
  }
  object Initial {
    def forNetwork(network: Network, peersCount: Int = 10): Initial = new Initial(
      network = network,
      address = NetworkAddress(InetAddress.getByName("localhost"), network),
      services = Services(),
      userAgent = "/Machinomics:0.0.1",
      relayBeforeFilter = false,
      peersCount = peersCount
    )
  }

  case class Working(network: Network,
                     address: NetworkAddress,
                     services: Services,
                     userAgent: String,
                     relayBeforeFilter: Boolean,
                     herd: ActorRef) extends NodeState
}
