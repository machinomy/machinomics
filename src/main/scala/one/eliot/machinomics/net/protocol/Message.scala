package one.eliot.machinomics.net.protocol

import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.codecs._
import scodec.bits._
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest

case class Message[A <: Payload : Codec](magic: Long, payload: A)

object Message {
  val commandEncoding = paddedFixedSizeBytes(12, ascii, constant(hex"00"))

  def codec[A <: Payload : Codec]: Codec[Message[A]] = {
    def encode(m: Message[A]): Attempt[BitVector] = {
      for {
        payloadBits <- implicitly[Codec[A]].encode(m.payload)
        payloadBytes = payloadBits.toByteVector
        magic <- uint32L.encode(m.magic)
        command <- commandEncoding.encode(m.payload.command)
        length <- uint32L.encode(payloadBytes.length)
        checksum <- uint32L.encode(checksum(payloadBytes))
      } yield magic ++ command ++ length ++ checksum ++ payloadBits
    }

    def decode(bits: BitVector): Attempt[DecodeResult[Message[A]]] =
      for {
        magicR <- uint32L.decode(bits)
        magic = magicR.value
        commandR <- commandEncoding.decode(magicR.remainder)
        lengthR <- uint32L.decode(commandR.remainder)
        checksumR <- uint32L.decode(lengthR.remainder)
        payloadBytesR <- bytes(lengthR.value.toInt).decode(checksumR.remainder).flatMap { p =>
          if (checksum(p.value) == checksumR.value) Successful(p) else Failure(scodec.Err("Checksum does not match"))
        }
        remainder = payloadBytesR.remainder
        payloadR <- implicitly[Codec[A]].decode(payloadBytesR.value.toBitVector)
        payload = payloadR.value
      } yield DecodeResult(Message(magic, payload), remainder)

    Codec(encode _, decode _)
  }

  def checksum(data: BitVector): Long = checksum(data.toByteVector)

  def checksum(data: ByteVector): Long = {
    val hash = hashBytes(data.toArray)
    val padding: Array[Byte] = Array.fill(4)(0)
    val byteBuffer = ByteBuffer.wrap(hash.slice(0, 4) ++ padding)
      .order(ByteOrder.LITTLE_ENDIAN)
    byteBuffer.getLong()
  }

  def hashBytes(bytes: Array[Byte]): Array[Byte] = {
    val messageDigest = MessageDigest.getInstance("SHA-256")
    def hash(bytes: Array[Byte]) = messageDigest.digest(bytes)
    hash(hash(bytes))
  }
}
