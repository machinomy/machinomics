package one.eliot.machinomics.store

import one.eliot.machinomics.Hash
import scodec.Attempt.Successful
import scodec.Codec
import scodec.codecs._
import scodec.bits.{ByteVector, BitVector}

import scala.collection.mutable

case class TXInput(prevTransactionHash: Hash,
                   prevTXOutIndex: Int,
                   txInScriptLength: Long,
                   txInScriptSig: Array[Byte],
                   sequenceNo: Int)

case class TXOutput(value: Array[Byte],
                    txOutScriptLength: Long,
                    txOutScriptPubKey: Array[Byte])

case class Transaction(version: Int,
                       inCounter: Long,
                       outCounter: Long,
                       txIn: Array[TXInput],
                       txOut: TXOutput,
                       lockTime: Int)

case class BlockHeader(version: Int,
                       prevBlockHash: Hash,
                       merkleRoot: Hash,
                       timestamp: Long,
                       bits: Long,
                       nonce: Long,
                       txCount: Int) {

  lazy val hash: Hash = {
    Array.empty[Byte]
  }

  override def toString: String = {
    s"""BlockHeader(version: $version, prevBlockHash: ${prevBlockHash.reverse.map("%02x".format(_)).mkString}, merkleRoot: ${merkleRoot.map("%02x".format(_)).mkString}, timestamp: $timestamp, bits: $bits, nonce: $nonce, txCount: $txCount)"""
  }
}

object BlockHeader {

  type Wire = Int ~ ByteVector ~ ByteVector ~ Long ~ Long ~ Long ~ Int

  val encoding: Codec[Wire] = int32L ~ bytes(32) ~ bytes(32) ~ uint32L ~ uint32L ~ uint32L ~ vintL

  def encode(m: BlockHeader): Wire =
    m.version ~ ByteVector(m.prevBlockHash) ~ ByteVector(m.merkleRoot) ~ m.timestamp ~ m.bits ~ m.nonce ~ m.txCount


  def decode(w: Wire): BlockHeader = w match {

    case version ~ prevBlockHash ~ merkleRoot ~ timestamp ~ bits ~ nonce ~ txCount =>
      new BlockHeader(version, prevBlockHash.toArray, merkleRoot.toArray, timestamp, bits, nonce, txCount)

  }

  implicit val codec: Codec[BlockHeader] = encoding.xmap(decode, encode)
}

case class Block(header: BlockHeader, transactions: Array[Transaction])

trait BlockStore[T] {
  def put(block: T): Unit = ???

  def get(hash: Hash): T = ???
}

class InMemoryBlockStore extends BlockStore[Block] {

  private val _collection: mutable.HashMap[Hash, Block] = mutable.HashMap.empty[Hash, Block]

  override def put(block: Block): Unit = {
    _collection += (block.header.hash -> block)
  }

  override def get(hash: Hash): Block = {
    _collection.getOrElse(hash, null)
  }
}