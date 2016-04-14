package one.eliot.machinomics.blockchain

case class Block(header: BlockHeader, transactions: Array[Transaction])
