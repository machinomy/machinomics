package one.eliot.machinomics.store

import one.eliot.machinomics.blockchain.{Block, DoubleHash}

import scala.collection.mutable

class InMemoryBlockStore extends BlockStore[Block] {

  private val _collection: mutable.HashMap[DoubleHash, Block] = mutable.HashMap.empty

  override def put(block: Block): Unit = {
    _collection += (block.header.hash -> block)
  }

  override def get(hash: DoubleHash): Block = {
    _collection.getOrElse(hash, null)
  }
}
