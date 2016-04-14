package one.eliot.machinomics.blockchain

import one.eliot.machinomics.Hash
import scodec.Codec
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs.{~, _}

case class BlockHeader(version: Int,
                       prevBlockHash: DoubleHash,
                       merkleRoot: DoubleHash,
                       timestamp: Long,
                       bits: Long,
                       nonce: Long,
                       txCount: Int) {

  lazy val hash: DoubleHash = {
    val bytes = Hash.doubleSHA256(Codec.encode(this).getOrElse(BitVector.empty).toByteArray.slice(0, 80)).reverse
    DoubleHash(bytes)
  }

  override def toString: String = {
    s"""BlockHeader(version: $version, prevBlockHash: ${prevBlockHash.toString}, merkleRoot: ${merkleRoot.toString}, timestamp: $timestamp, bits: $bits, nonce: $nonce, txCount: $txCount)"""
  }
}


object BlockHeader {

  type Wire = Int ~ ByteVector ~ ByteVector ~ Long ~ Long ~ Long ~ Int

  val encoding: Codec[Wire] = int32L ~ bytes(32) ~ bytes(32) ~ uint32L ~ uint32L ~ uint32L ~ vintL

  def encode(m: BlockHeader): Wire =
    m.version ~ ByteVector(m.prevBlockHash.bytes).reverse ~ ByteVector(m.merkleRoot.bytes).reverse ~ m.timestamp ~ m.bits ~ m.nonce ~ m.txCount


  def decode(w: Wire): BlockHeader = w match {

    case version ~ prevBlockHash ~ merkleRoot ~ timestamp ~ bits ~ nonce ~ txCount =>
      new BlockHeader(version, DoubleHash(prevBlockHash.reverse.toArray), DoubleHash(merkleRoot.reverse.toArray), timestamp, bits, nonce, txCount)

  }

  implicit val codec: Codec[BlockHeader] = encoding.xmap(decode, encode)
}
