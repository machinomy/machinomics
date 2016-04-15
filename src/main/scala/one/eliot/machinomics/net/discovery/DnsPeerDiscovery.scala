package one.eliot.machinomics.net.discovery

import java.net.{InetAddress, InetSocketAddress}

import com.typesafe.scalalogging.LazyLogging
import one.eliot.machinomics.net.Network

import scala.concurrent.Future
import scala.util.Try

class DnsPeerDiscovery(seed: DnsSeed, network: Network) extends PeerDiscovery[DnsSeed] with LazyLogging {
  override def peers(): Future[Seq[InetSocketAddress]] = {
    Try(InetAddress.getAllByName(seed.hostname)).toOption match {
      case Some(inetAddresses) => {
        logger.info(s"Resolved ${inetAddresses.size} addresses for ${seed.hostname}")
        val socketAddresses = inetAddresses.map(new InetSocketAddress(_, network.port))
        Future.successful(socketAddresses)
      }
      case None => Future.successful(Seq.empty)
    }
  }
}
