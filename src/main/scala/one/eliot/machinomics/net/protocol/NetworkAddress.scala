package one.eliot.machinomics.net.protocol

import java.net.{InetSocketAddress, InetAddress}

import one.eliot.machinomics.net.{Network, ProtocolVersion}
import org.joda.time.DateTime
import scodec._
import scodec.bits._
import codecs._

case class NetworkAddress(address: InetAddress,
                          port: Int,
                          services: Services = Services(),
                          protocolVersion: ProtocolVersion.Value = ProtocolVersion.CURRENT,
                          time: Option[DateTime] = None)

object NetworkAddress {
  def apply(address: InetAddress, network: Network) = new NetworkAddress(
    address = address,
    port = network.port,
    protocolVersion = network.protocolVersion
  )

  val ipv4pad: ByteVector = hex"00 00 00 00 00 00 00 00 00 00 FF FF"

  def encode(ia: InetAddress) = {
    val bts = ByteVector(ia.getAddress)
    if (bts.length == 4) bytes(16).encode(ipv4pad ++ bts) else  bytes(16).encode(bts)
  }

  def decode(buf: BitVector) = bytes(16).decode(buf).map { b =>
    val bts = if (b.value.take(12) == ipv4pad) b.value.drop(12) else b.value
    DecodeResult(InetAddress.getByAddress(bts.toArray), b.remainder)
  }

  implicit val inetAddressCodec = Codec[InetAddress](encode(_), decode(_))

  implicit val inetSocketAddressCodec: Codec[InetSocketAddress] = (inetAddressCodec ~ uint16).xmap(
    new InetSocketAddress(_: InetAddress, _: Int),
    isa => (isa.getAddress, isa.getPort)
  )

  implicit val codec: Codec[NetworkAddress] = (implicitly[Codec[Services]] ~ implicitly[Codec[InetSocketAddress]]).xmap(
    { case (services, inetSocketAddress) => NetworkAddress(inetSocketAddress.getAddress, inetSocketAddress.getPort, services) },
    { networkAddress => (networkAddress.services, new InetSocketAddress(networkAddress.address, networkAddress.port)) }
  )
}
