package one.eliot.machinomics.net

import java.net.{Inet4Address, InetSocketAddress}

import akka.actor.{Props, Actor, ActorLogging}
import one.eliot.machinomics.blockchain.BlockHeader
import one.eliot.machinomics.net.discovery.PeerDiscovery

import scala.util.Random

class Herd(network: Network) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  override def receive = {
    case Herd.Connect(peerCount) =>
      val s = sender()
      log.info(s"Starting passive node for ${network.name}")
      val discovered = PeerDiscovery.forNetwork(network)
      discovered.onSuccess {
        case addresses: Seq[InetSocketAddress] =>
          val selected = Random.shuffle(addresses.filter(_.getAddress.isInstanceOf[Inet4Address])).take(peerCount)
          val peers = for (addr <- selected) yield context.actorOf(PeerConnectionA.props(addr, network))
          val initialState = new HerdStateA(s, network, HerdStateA.EMPTY, peers.toSet)
          next(initialState)
          notify(initialState, Herd.DidConnect())
      }

    case Herd.Disconnect() => log.info(s"Stopping passive node for ${network.name}")
  }

  def onReady(state: HerdStateA): Receive = {
    case Herd.Handshake() =>
      log.info(s"Start handshaking with ${state.mayBePeers.size} peers")
      next(state.copy(status = HerdStateA.CONNECTING))
      for (peer <- state.mayBePeers) { peer ! PeerConnectionA.ConnectCommand() }
  }

  def onConnecting(state: HerdStateA): Receive = {
    case PeerConnectionA.DidAcknowledge(peerState) =>
      log.info(s"Received acknowledgement from ${peerState.address}")
      val nextState = HerdStateA.connect(state, sender())
      if (nextState.status == HerdStateA.CONNECTED && state.status == HerdStateA.CONNECTING) {
        notify(nextState, Herd.DidHandshake())
      }
      next(nextState)

    case x => log.info(x.toString)
  }

  def onConnected(state: HerdStateA): Receive = {
    case Herd.GetHeaders() =>
      for (peer <- state.connectedPeers) {
        peer ! PeerConnectionA.HeadersQuery()
        next(HerdStateA.sendForHeaders(state, peer))
      }
  }

  def onGettingHeaders(state: HerdStateA): Receive = {
    case PeerConnectionA.GotHeaders(peerState, headers) =>
      log.info(s"Getting ${headers.length} headers")
      peerState match {
        case peerState: PeerState.GettingHeaders =>
          notify(state, Herd.GotHeaders(headers))
          next(state)
        case peerState: PeerState.Acknowledged =>
          notify(state, Herd.GotHeaders(headers))
          next(HerdStateA.finishGettingHeaders(state, sender()))
      }
  }

  def onSomethingUnexpected(state: HerdStateA): Receive = {
    case x: Any =>
      log.error(x.toString)
      throw new Herd.UnexpectedBehavior(state)
  }

  def next(state: HerdStateA): Unit = {
    state.status match {
      case HerdStateA.EMPTY =>
        context.become(onReady(state) orElse onSomethingUnexpected(state))
        println("context became onReady")
      case HerdStateA.CONNECTING =>
        context.become(onConnecting(state) orElse onSomethingUnexpected(state))
        println("context became onConnecting")
      case HerdStateA.CONNECTED =>
        context.become(onConnected(state) orElse onConnecting(state) orElse onSomethingUnexpected(state))
        println("context became onConnected")
      case HerdStateA.GETTING_HEADERS =>
        context.become(onGettingHeaders(state) orElse onConnecting(state) orElse onSomethingUnexpected(state))
        println("context became onGettingHeaders")
      case _ => onSomethingUnexpected(state)
    }
  }

  def notify(state: HerdStateA, message: Herd.Message) = state.node ! message

}

object Herd {

  def props(network: Network) = Props(classOf[Herd], network)

  sealed trait Message
  case class Connect(peerCount: Int = 5) extends Message
  case class Handshake() extends Message
  case class GetHeaders() extends Message
  case class Disconnect() extends Message

  sealed trait Notification extends Message
  case class DidConnect() extends Notification
  case class DidHandshake() extends Notification
  case class GotHeaders(headers: List[BlockHeader]) extends Notification

  class UnexpectedBehavior(state: HerdStateA) extends Throwable
}
