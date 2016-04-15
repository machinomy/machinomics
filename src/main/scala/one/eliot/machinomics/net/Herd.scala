package one.eliot.machinomics.net

import java.net.{Inet4Address, InetSocketAddress}

import akka.actor.{Actor, ActorLogging, ActorRef}

import scala.concurrent.Future
import scala.util.Random

class Herd(network: Network) extends Actor with ActorLogging {

  var peers: Seq[ActorRef] = Seq.empty

  implicit val ec = context.dispatcher

  override def receive = {
    case Herd.Connect() => {
      log.info(s"Starting passive node for ${network.name}")
      /*network.peers().onSuccess {
        case addresses: Seq[InetSocketAddress] =>
          val selected = Random.shuffle(addresses.filter(_.getAddress.isInstanceOf[Inet4Address])).take(5)
          peers = for (addr <- selected) yield context.actorOf(Peer.props)
      }*/
    }
    case Herd.Disconnect() => log.info(s"Stopping passive node for ${network.name}")
  }
}

object Herd {
  sealed trait Message
  case class Connect() extends Message
  case class Disconnect() extends Message
}
