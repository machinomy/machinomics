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
          val peers = for (addr <- selected) yield context.actorOf(Peer.props(addr, network))
          val initialState = new HerdState(s, network, HerdState.EMPTY, peers.toSet)
          next(initialState)
          notify(initialState, Herd.DidConnect())
      }

    case Herd.Disconnect() => log.info(s"Stopping passive node for ${network.name}")
  }

  def onReady(state: HerdState): Receive = {
    case Herd.Handshake() =>
      log.info(s"Start handshaking with ${state.mayBePeers.size} peers")
      next(state.copy(status = HerdState.CONNECTING))
      for (peer <- state.mayBePeers) { peer ! Peer.ConnectCommand() }
  }

  def onConnecting(state: HerdState): Receive = {
    case Peer.DidAcknowledge(peerState) =>
      log.info(s"Received acknowledgement from ${peerState.address}")
      val nextState = HerdState.connect(state, sender())
      if (nextState.status == HerdState.CONNECTED && state.status == HerdState.CONNECTING) {
        notify(nextState, Herd.DidHandshake())
      }
      next(nextState)

    case x => log.info(x.toString)
  }

  def onConnected(state: HerdState): Receive = {
    case Herd.GetHeaders() =>
      for (peer <- state.connectedPeers) {
        peer ! Peer.HeadersQuery()
        next(HerdState.sendForHeaders(state, peer))
      }
  }

  def onGettingHeaders(state: HerdState): Receive = {
    case Peer.GotHeaders(peerState, headers) =>
      log.info(s"Getting ${headers.length} headers")
      peerState match {
        case peerState: PeerState.GettingHeaders =>
          notify(state, Herd.GotHeaders(headers))
          next(state)
        case peerState: PeerState.Acknowledged =>
          notify(state, Herd.GotHeaders(headers))
          next(HerdState.finishGettingHeaders(state, sender()))
      }
  }

  def onSomethingUnexpected(state: HerdState): Receive = {
    case x: Any =>
      log.error(x.toString)
      throw new Herd.UnexpectedBehavior(state)
  }

  def next(state: HerdState): Unit = {
    state.status match {
      case HerdState.EMPTY =>
        context.become(onReady(state) orElse onSomethingUnexpected(state))
        println("context became onReady")
      case HerdState.CONNECTING =>
        context.become(onConnecting(state) orElse onSomethingUnexpected(state))
        println("context became onConnecting")
      case HerdState.CONNECTED =>
        context.become(onConnected(state) orElse onConnecting(state) orElse onSomethingUnexpected(state))
        println("context became onConnected")
      case HerdState.GETTING_HEADERS =>
        context.become(onGettingHeaders(state) orElse onConnecting(state) orElse onSomethingUnexpected(state))
        println("context became onGettingHeaders")
      case _ => onSomethingUnexpected(state)
    }
  }

  def notify(state: HerdState, message: Herd.Message) = state.node ! message

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

  class UnexpectedBehavior(state: HerdState) extends Throwable
}
