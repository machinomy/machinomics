package one.eliot.machinomics.net.protocol

import java.net.InetAddress
import com.github.nscala_time.time.Imports._
import one.eliot.machinomics.net.Network
import scodec._
import codecs._


case class VersionPayload(version: Int,
                          services: Services = Services(),
                          timestamp: Long,
                          theirAddress: NetworkAddress,
                          myAddress: NetworkAddress,
                          nonce: Long,
                          userAgent: String,
                          height: Int = 0,
                          relayBeforeFilter: Boolean = true) extends Payload("version")

object VersionPayload {
  def apply(network: Network, address: InetAddress) = {
    val version = network.protocolVersion.number
    val timestamp = (DateTime.now.getMillis / 1000).toInt
    val theirAddress = NetworkAddress(address, network)
    val myAddress = NetworkAddress(InetAddress.getLocalHost, network)
    val userAgent = "/Machinomics:0.0.1"
    new VersionPayload(version, Services(), timestamp, theirAddress, myAddress, 0, userAgent, 0, false)
  }

  type Wire = Int ~ Services ~ Long ~ NetworkAddress ~ NetworkAddress ~ Long ~ String ~ Int ~ Boolean
  val encoding: Codec[Wire] =
      int32L ~
        implicitly[Codec[Services]] ~
        int64L ~
        implicitly[Codec[NetworkAddress]] ~
        implicitly[Codec[NetworkAddress]] ~
        int64L ~
        variableSizeBytes(vintL, ascii) ~
        int32L ~
        bool(8)

  def encode(m: VersionPayload): Wire =
    m.version ~
      m.services ~
      m.timestamp ~
      m.theirAddress ~
      m.myAddress ~
      m.nonce ~
      m.userAgent ~
      m.height ~
      m.relayBeforeFilter

  def decode(w: Wire): VersionPayload = w match {
    case version ~ services ~ timestamp ~ theirAddress ~ myAddress ~ nonce ~ userAgent ~ height ~ relayBeforeFilter =>
      VersionPayload(version, services, timestamp, theirAddress, myAddress, nonce, userAgent, height, relayBeforeFilter)
  }

  implicit val codec: Codec[VersionPayload] = encoding.xmap(decode(_), encode(_))
}
