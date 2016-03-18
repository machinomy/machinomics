package one.eliot.machinomics.examples

import akka.actor.{Props, ActorSystem}
import one.eliot.machinomics.net.{Node, Testnet3Network, PassiveNode}

object PrintBlockchainHeights extends App {
  implicit val actorSystem = ActorSystem("foo")
  val network = Testnet3Network
  val node = actorSystem.actorOf(Props(classOf[PassiveNode], network))
  node ! Node.Start()
}
