package one.eliot.machinomics.blockchain

case class TxInput(prevTransactionHash: DoubleHash,
                   prevTxOutIndex: Int,
                   txInScriptLength: Long,
                   txInScriptSig: Array[Byte],
                   sequenceNo: Int)
