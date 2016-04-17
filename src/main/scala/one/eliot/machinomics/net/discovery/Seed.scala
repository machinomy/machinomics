package one.eliot.machinomics.net.discovery

import one.eliot.machinomics.net.Network

sealed trait Seed
case class DnsSeed(hostname: String) extends Seed

object Seed {
  def discovery[A <: Seed](seed: A, network: Network) = seed match {
    case s: DnsSeed => new DnsPeerDiscovery(s, network)
  }
}
