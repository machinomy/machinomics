package one.eliot.machinomics.examples

import one.eliot.machinomics.net.Testnet3Network
import one.eliot.machinomics.net.discovery.PeerDiscovery

import scala.concurrent.ExecutionContext.Implicits.global

object PrintDnsPeers extends App {
  val network = Testnet3Network
  val peers = PeerDiscovery.forNetwork(network)
  peers.onSuccess {
    case addresses => println(addresses)
  }
  peers.onFailure {
    case e => println(e)
  }
}
