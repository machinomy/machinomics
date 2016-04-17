package one.eliot.machinomics.net.discovery

import one.eliot.machinomics.net.Network
import java.net.InetSocketAddress

import scala.concurrent.{ExecutionContext, Future}

trait PeerDiscovery[A <: Seed] {
  def peers(): Future[Seq[InetSocketAddress]]
}

object PeerDiscovery {
  def forNetwork(network: Network)(implicit ec: ExecutionContext): Future[Seq[InetSocketAddress]] = {
    val listOfFutures: Seq[Future[Seq[InetSocketAddress]]] = network.seeds.map(s => Seed.discovery(s, network).peers)
    Future.sequence(listOfFutures).map(_.flatten)
  }
}
