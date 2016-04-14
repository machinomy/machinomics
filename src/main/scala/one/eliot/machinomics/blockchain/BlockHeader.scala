package one.eliot.machinomics.blockchain

import scodec.Codec
import scodec.bits._
import scodec.codecs._

case class BlockHeader(version: Int,
                       prevBlockHash: DoubleHash,
                       merkleRoot: DoubleHash,
                       timestamp: Long,
                       bits: Long,
                       nonce: Long,
                       txCount: Int) {

  lazy val hash = BlockHeader.hash(this)

  override def toString: String = {
    s"""BlockHeader(version: $version, prevBlockHash: ${prevBlockHash.toString}, merkleRoot: ${merkleRoot.toString}, timestamp: $timestamp, bits: $bits, nonce: $nonce, txCount: $txCount)"""
  }
}


object BlockHeader {

  def hash(blockHeader: BlockHeader) = {
    val bytes = Codec.encode(blockHeader).getOrElse(BitVector.empty).toByteArray.slice(0, 80)
    DoubleHash.digest(bytes)
  }

  type Wire = Int ~ DoubleHash ~ DoubleHash ~ Long ~ Long ~ Long ~ Int

  val encoding: Codec[Wire] = int32L ~ implicitly[Codec[DoubleHash]] ~ implicitly[Codec[DoubleHash]] ~ uint32L ~ uint32L ~ uint32L ~ vintL

  def encode(m: BlockHeader): Wire =
    m.version ~ m.prevBlockHash ~ m.merkleRoot ~ m.timestamp ~ m.bits ~ m.nonce ~ m.txCount


  def decode(w: Wire): BlockHeader = w match {

    case version ~ prevBlockHash ~ merkleRoot ~ timestamp ~ bits ~ nonce ~ txCount =>
      new BlockHeader(version, prevBlockHash, merkleRoot, timestamp, bits, nonce, txCount)

  }

  implicit val codec: Codec[BlockHeader] = encoding.xmap(decode, encode)
}
