package one.eliot.machinomics.blockchain

case class Transaction(version: Int,
                       inCounter: Long,
                       outCounter: Long,
                       txIn: Array[TxInput],
                       txOut: TxOutput,
                       lockTime: Int)
