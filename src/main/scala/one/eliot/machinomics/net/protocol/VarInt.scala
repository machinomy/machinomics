package one.eliot.machinomics.net.protocol

import scodec._
import scodec.bits.{BitVector, _}
import scodec.codecs._

case class VarInt(v: BigInt) {
  def toInt: Int = v.toInt
}

object VarInt {
  def encode(varInt: VarInt): Attempt[BitVector] = {
    def bigIntToLong(n: BigInt): Long = {
      val smallestBit = (n & 1).toLong
      ((n >> 1).toLong << 1) | smallestBit
    }
    varInt.v match {
      case v if v < 0xFD => uint8L.encode(v.toInt)
      case v if v <= 0xFFFF => uint16L.encode(v.toInt).map(e => BitVector(0xFD) ++ e)
      case v if v <= 0xFFFFFFFF => uint32L.encode(v.toInt).map(e => BitVector(0xFE) ++ e)
      case v => int64L.encode(bigIntToLong(v))
    }
  }

  def decode(bitVector: BitVector): Attempt[DecodeResult[VarInt]] = {
    def longToBigInt(unsignedLong: Long): BigInt = (BigInt(unsignedLong >>> 1) << 1) + (unsignedLong & 1)
    def longToVarInt(unsignedLong: Long): VarInt = VarInt(longToBigInt(unsignedLong))

    bitVector.toByteArray.toList match {
      case -1 :: v => int64L.decode(BitVector(v)).map(_.map(longToVarInt))
      case -2 :: v => uint32L.decode(BitVector(v)).map(_.map(longToVarInt))
      case -3 :: v => uint16L.decode(BitVector(v)).map(_.map(i => VarInt(longToBigInt(i))))
      case v => uint8L.decode(BitVector(v)).map(_.map(i => VarInt(longToBigInt(i))))
    }
  }

  implicit val codec: Codec[VarInt] = Codec(encode(_), decode(_))
}
