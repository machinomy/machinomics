package one.eliot.machinomics.net

case class Node(state: NodeState)

object Node {
  def apply(network: Network): Node = Node(NodeState(network))
}
