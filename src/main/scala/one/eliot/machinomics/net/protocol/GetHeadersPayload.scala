package one.eliot.machinomics.net.protocol

import one.eliot.machinomics.blockchain.DoubleHash
import one.eliot.machinomics.net.ProtocolVersion
import scodec._
import scodec.codecs._

case class GetHeadersPayload(version: ProtocolVersion.Value,
                             hashCount: Long,
                             blockLocatorHashes: List[DoubleHash],
                             hashStop: DoubleHash) extends Payload("getheaders")

object GetHeadersPayload {
  def apply(blockLocatorHashes: List[DoubleHash]) = {
    new GetHeadersPayload(version             = ProtocolVersion.CURRENT,
                          hashCount           = blockLocatorHashes.length,
                          blockLocatorHashes  = blockLocatorHashes,
                          hashStop            = DoubleHash.zero)
  }

  type Wire = Int ~ VarInt ~ List[DoubleHash] ~ DoubleHash
  val encoding: Codec[Wire] = int32L ~ VarInt.codec ~ list(implicitly[Codec[DoubleHash]]) ~ implicitly[Codec[DoubleHash]]


  def encode(m: GetHeadersPayload): Wire =
    m.version.number ~ VarInt(m.hashCount) ~ m.blockLocatorHashes ~ m.hashStop


  def decode(w: Wire): GetHeadersPayload = w match {
    case version ~ hashCount ~ blockLocatorHashes ~ hashStop =>
      GetHeadersPayload(ProtocolVersion.Value(version), hashCount.toInt, blockLocatorHashes, hashStop)
  }


  implicit val codec: Codec[GetHeadersPayload] = encoding.xmap(decode, encode)
}
