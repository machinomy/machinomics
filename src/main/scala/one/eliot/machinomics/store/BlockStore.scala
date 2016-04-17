package one.eliot.machinomics.store

import one.eliot.machinomics.blockchain.DoubleHash

trait BlockStore[T] {
  def put(block: T): Unit

  def get(hash: DoubleHash): Option[T]
}
