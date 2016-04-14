package one.eliot.machinomics.net

import java.net.InetAddress

case class NodeState(network: Network,
                     address: NetworkAddress,
                     services: Services,
                     userAgent: String,
                     relayBeforeFilter: Boolean)

object NodeState {
  def apply(network: Network) = new NodeState(
    network = network,
    address = NetworkAddress(InetAddress.getByName("localhost"), network),
    services = Services(),
    userAgent = "/Machinomics:0.0.1",
    relayBeforeFilter = false
  )
}
