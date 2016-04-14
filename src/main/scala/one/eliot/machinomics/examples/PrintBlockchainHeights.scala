package one.eliot.machinomics.examples

import akka.actor.{Props, ActorSystem}
import one.eliot.machinomics.net.{Herd, Testnet3Network}

object PrintBlockchainHeights extends App {
  implicit val actorSystem = ActorSystem("foo")
  val network = Testnet3Network
  val node = actorSystem.actorOf(Props(classOf[Herd], network))
  node ! Herd.Connect()
}
