package one.eliot.machinomics.blockchain

case class TxOutput(value: Array[Byte],
                    txOutScriptLength: Long,
                    txOutScriptPubKey: Array[Byte])
