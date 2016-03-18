package one.eliot.machinomics.net.discovery

import java.net.InetSocketAddress

import one.eliot.machinomics.net.Network

import scala.concurrent.Future

trait Discovery[A <: Seed] {
  def peers(network: Network): Future[Seq[InetSocketAddress]]
}
