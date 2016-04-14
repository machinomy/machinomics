package one.eliot.machinomics.net

import java.net.InetSocketAddress

import one.eliot.machinomics.blockchain.DoubleHash
import one.eliot.machinomics.net.discovery.{DnsSeed, Seed}

import scala.concurrent.{ExecutionContext, Future}

sealed trait Network {
  val name: String
  val magic: Int
  val addressHeader: Int
  val p2shHeader: Int
  val seeds: Seq[Seed]
  val port: Int
  val protocolVersion = ProtocolVersion.CURRENT
  val genesisHash: DoubleHash

  def peers()(implicit ec: ExecutionContext): Future[Seq[InetSocketAddress]] = {
    val listOfFutures: Seq[Future[Seq[InetSocketAddress]]] = seeds.map(s => s.discovery.peers(this))
    Future.sequence(listOfFutures).map(_.flatten)
  }
}

object Testnet3Network extends Network {
  override val name = "testnet3"
  override val magic = 0x0709110B
  override val addressHeader = 0x6f
  override val p2shHeader = 196
  override val seeds = Seq(
    DnsSeed("testnet-seed.bitcoin.schildbach.de"),
    DnsSeed("testnet-seed.bitcoin.petertodd.org"),
    DnsSeed("localhost")
  )
  override val port: Int = 18333
  override val genesisHash = DoubleHash.fromHex("0000000005bdbddb59a3cd33b69db94fa67669c41d9d32751512b5d7b68c71cf")
}

object MainNetwork extends Network {
  override val name = "mainnet"
  override val magic = 0xD9B4BEF9
  override val addressHeader = 0
  override val p2shHeader = 5
  override val seeds = Seq(
    DnsSeed("seed.bitcoin.sipa.be"),        // Pieter Wuille
    DnsSeed("dnsseed.bluematt.me"),         // Matt Corallo
    DnsSeed("dnsseed.bitcoin.dashjr.org"),  // Luke Dashjr
    DnsSeed("seed.bitcoinstats.com"),       // Chris Decker
    DnsSeed("seed.bitnodes.io")             // Addy Yeow
  )
  override val port: Int = 8333
  override val genesisHash = DoubleHash.fromHex("000000000019d6689c085ae165831e934ff763ae46a2a6c172b3f1b60a8ce26f")
}
