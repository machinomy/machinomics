package one.eliot.machinomics.store

import one.eliot.machinomics.blockchain.{Block, DoubleHash}
import org.mapdb._
import scodec.Codec
import scodec.bits.BitVector

class MapsDbBlockStore extends BlockStore[Block] {
  val db = DBMaker.fileDB("blockstore").make()
  val blocks: HTreeMap[Array[Byte], Array[Byte]] = db.hashMap("blocks", Serializer.BYTE_ARRAY, Serializer.BYTE_ARRAY).make()

  override def put(block: Block): Unit = {
    for {
      hashEncoded <- Codec.encode(block.header.hash).toOption
      blockEncoded <- Codec.encode(block).toOption
    } yield {
      blocks.put(hashEncoded.toByteArray, blockEncoded.toByteArray)
      db.commit()
    }
  }

  override def get(hash: DoubleHash): Option[Block] = {
    for {
      hashEncoded <- Codec.encode(hash).toOption
      retrievedBytes <- Option(blocks.get(hashEncoded.toByteArray))
      blockDecodeResult <- Codec.decode[Block](BitVector(retrievedBytes)).toOption
    } yield blockDecodeResult.value
  }

  def close(): Unit = db.close()
}
