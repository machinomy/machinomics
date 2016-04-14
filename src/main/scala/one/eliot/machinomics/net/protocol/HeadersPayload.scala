package one.eliot.machinomics.net.protocol

import scodec._
import codecs._
import one.eliot.machinomics.blockchain.BlockHeader

case class HeadersPayload(count: Int, headers: List[BlockHeader] = List.empty) extends Payload("headers")


object HeadersPayload {

  type Wire = VarInt ~ List[BlockHeader]

  val encoding: Codec[Wire] = implicitly[Codec[VarInt]] ~ list(implicitly[Codec[BlockHeader]])

  def encode(m: HeadersPayload): Wire = VarInt(m.count) ~ m.headers

  def decode(w: Wire): HeadersPayload = w match {
    case count ~ headers => new HeadersPayload(count.toInt, headers)
  }

  implicit val codec: Codec[HeadersPayload] = encoding.xmap(decode, encode)
}
