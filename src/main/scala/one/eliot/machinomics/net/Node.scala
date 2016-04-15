package one.eliot.machinomics.net

import java.net.InetAddress

class Node(network: Network,
           address: NetworkAddress,
           services: Services,
           userAgent: String,
           relayBeforeFilter: Boolean)

object Node {
  def apply(network: Network): Node = new Node(
    network = network,
    address = NetworkAddress(InetAddress.getByName("localhost"), network),
    services = Services(),
    userAgent = "/Machinomics:0.0.1",
    relayBeforeFilter = false
  )
}
