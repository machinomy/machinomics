package one.eliot.machinomics.net

case class NodeState(network: Network,
                     address: NetworkAddress,
                     services: Services,
                     userAgent: String,
                     relayBeforeFilter: Boolean)
