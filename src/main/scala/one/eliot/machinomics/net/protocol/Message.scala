package one.eliot.machinomics.net.protocol

import scodec._
import scodec.codecs._
import scodec.bits._

case class Message[A <: Payload : Codec](magic: Long, payload: A)

object Message {
  val commandEncoding = paddedFixedSizeBytes(12, ascii, constant(hex"00"))
  def encoding[A <: Payload : Codec]: Codec[Long ~ String ~ Long ~Long ~ A] = uint32L ~ commandEncoding ~ uint32L ~ uint32L ~ implicitly[Codec[A]]

  implicit def codec[A <: Payload : Codec]: Codec[Message[A]] = encoding.xmap(
    { case magick ~ command ~ length ~ checksum ~ payload => Message(magick, payload) },
    m => m.magic ~ m.payload.command ~ payloadLength(m) ~ payloadChecksum(m) ~ m.payload
  )

  def payloadLength
}
