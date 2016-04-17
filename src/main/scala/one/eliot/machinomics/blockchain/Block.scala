package one.eliot.machinomics.blockchain

import scodec._

case class Block(header: BlockHeader, transactions: Array[Transaction])

object Block {
  implicit val codec: Codec[Block] = implicitly[Codec[BlockHeader]].xmap(
    { case header: BlockHeader => Block(header, Array.empty) },
    { case block: Block => block.header }
  )
}
