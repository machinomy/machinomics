package one.eliot.machinomics.net.protocol

import one.eliot.machinomics.store.BlockHeader
import scodec._
import codecs._

case class HeadersPayload(count: Int, headers: List[BlockHeader] = List.empty[BlockHeader]) extends Payload("headers")


object HeadersPayload {

  type Wire = VarInt ~ List[BlockHeader]

  val encoding: Codec[Wire] = VarInt.codec ~ list(implicitly[Codec[BlockHeader]])

  def encode(m: HeadersPayload): Wire = VarInt(m.count) ~ m.headers

  def decode(w: Wire): HeadersPayload = w match {

    case count ~ headers =>
      new HeadersPayload(count.toInt, headers)

  }

  implicit val codec: Codec[HeadersPayload] = encoding.xmap(decode, encode)
}
