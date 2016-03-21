package one.eliot.machinomics.net.protocol

import scodec._
import scodec.bits.BitVector
import scodec.codecs._

case class VerackPayload() extends Payload("verack")

object VerackPayload {
  implicit val codec: Codec[VerackPayload] = constant(BitVector.empty).xmap(_ => VerackPayload(), _ => ())
}
