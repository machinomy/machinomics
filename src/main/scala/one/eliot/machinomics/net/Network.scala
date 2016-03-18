package one.eliot.machinomics.net

import java.net.{InetAddress, InetSocketAddress}

import one.eliot.machinomics.net.discovery.{Seed, DnsSeed}

import scala.concurrent.{ExecutionContext, Future}

sealed trait Network {
  val name: String
  val magick: Int
  val addressHeader: Int
  val p2shHeader: Int
  val seeds: Seq[Seed]
  val port: Int
  val protocolVersion = ProtocolVersion.CURRENT

  def peers()(implicit ec: ExecutionContext): Future[Seq[InetSocketAddress]] = {
    /*val listOfFutures: Seq[Future[Seq[InetSocketAddress]]] = seeds.map(s => s.discovery.peers(this))
    Future.sequence(listOfFutures).map(_.flatten)*/
    val address = new InetSocketAddress(InetAddress.getByName("localhost"), port)
    Future.successful(Seq(address))
  }
}

object Testnet3Network extends Network {
  override val name = "testnet3"
  override val magick = 0xDAB5BFFA
  override val addressHeader = 111
  override val p2shHeader = 196
  override val seeds = Seq(DnsSeed("testnet-seed.bitcoin.schildbach.de"), DnsSeed("testnet-seed.bitcoin.petertodd.org"))
  override val port: Int = 18333
}
