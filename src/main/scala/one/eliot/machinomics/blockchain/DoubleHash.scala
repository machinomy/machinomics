package one.eliot.machinomics.blockchain

import java.security.MessageDigest

import scodec._
import scodec.codecs._
import scodec.bits._

case class DoubleHash(bytes: Array[Byte]) {
  override def toString = {
    bytes.map("%02x".format(_)).mkString
  }

  def toByteArray = bytes
}

object DoubleHash {
  def digest(bytes: Array[Byte]): DoubleHash = {
    def sha256(bytes: Array[Byte]): Array[Byte] = {
      val messageDigest = MessageDigest.getInstance("SHA-256")
      messageDigest.update(bytes)
      messageDigest.digest()
    }

    DoubleHash(sha256(sha256(bytes)).reverse)
  }

  def zero: DoubleHash = DoubleHash(Array.empty)

  implicit val codec: Codec[DoubleHash] = bytes(32).xmap(
    { case byteVector => DoubleHash(byteVector.reverse.toArray) },
    { case DoubleHash(bytes) => ByteVector(bytes.reverse) }
  )
}
