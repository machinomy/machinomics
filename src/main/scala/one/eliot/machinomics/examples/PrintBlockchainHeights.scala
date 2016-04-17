package one.eliot.machinomics.examples

import akka.actor.{Props, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import one.eliot.machinomics.net.{Herd, Testnet3Network}

object PrintBlockchainHeights extends App {
  implicit val actorSystem = ActorSystem("foo")
  implicit val timeout = Timeout(1.hour)
  val network = Testnet3Network
  val node = actorSystem.actorOf(Props(classOf[Herd], network))
  val future =for {
    connected <- node ? Herd.Connect(30)
    handshaked <- node ? Herd.Handshake()
  } yield handshaked

  Await.result(future, Duration.Inf)
  println(future)
}
