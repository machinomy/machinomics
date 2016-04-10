package one.eliot.machinomics.net.protocol

import one.eliot.machinomics.Hash
import one.eliot.machinomics.net.ProtocolVersion
import scodec.Codec
import scodec.bits.ByteVector
import scodec.codecs._

case class GetHeadersPayload(version: ProtocolVersion.Value,
                             hashCount: Long,
                             blockLocatorHashes: List[Hash],
                             hashStop: Hash) extends Payload("getheaders")

object GetHeadersPayload {
  def apply(blockLocatorHashes: List[Hash]) = {
    new GetHeadersPayload(version             = ProtocolVersion.CURRENT,
                          hashCount           = blockLocatorHashes.length,
                          blockLocatorHashes  = blockLocatorHashes,
                          hashStop            = Array[Byte](0))
  }

  type Wire = Int ~ VarInt ~ List[ByteVector] ~ ByteVector
  val encoding: Codec[Wire] = int32L ~ VarInt.codec ~ list(bytes(32)) ~ bytes(32)


  def encode(m: GetHeadersPayload): Wire =
    m.version.number ~ VarInt(m.hashCount) ~ m.blockLocatorHashes.map(ByteVector(_).reverse) ~ ByteVector(m.hashStop)


  def decode(w: Wire): GetHeadersPayload = w match {

    case version ~ hashCount ~ blockLocatorHashes ~ hashStop =>
      GetHeadersPayload(ProtocolVersion.Value(version), hashCount.toInt, blockLocatorHashes.map(_.toArray), hashStop.toArray)

  }


  implicit val codec: Codec[GetHeadersPayload] = encoding.xmap(decode, encode)
}
