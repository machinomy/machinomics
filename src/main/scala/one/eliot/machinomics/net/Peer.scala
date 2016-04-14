package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import one.eliot.machinomics.net.protocol._
import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.bits._

class Peer(remote: InetSocketAddress, network: Network) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  var services:   Services = Services()
  var version:    ProtocolVersion.Value = network.protocolVersion
  var selfReportedAddress:    NetworkAddress = NetworkAddress(remote, network)
  var userAgent:  String = ""
  var height:     Int = 0

  var receivedNumber = 0

  var buffer = ByteString.newBuilder

  var blockHeaderCount = 0

  IO(Tcp) ! Connect(remote)

  override def receive = onConnected orElse onFailedConnection orElse onSomethingUnexpected

  def onConnected: Actor.Receive = { case c @ Connected(r, l) =>
    log.info(s"Connected to $r")
    sender ! Register(self)
    sendMessage(protocol.VersionPayload(network, remote.getAddress))
    val initialState = PeerState.Initial(network, NetworkAddress(remote, network))
    context.become(onVersionPayloadReceived(initialState) orElse onSomethingUnexpected)
  }

  def onVersionPayloadReceived(state: PeerState.Initial): Actor.Receive = onMessageReceived[protocol.VersionPayload] { payload =>
    log.info(s"Done version handshake with ${state.address}")
    sendMessage(protocol.VerackPayload())
    val nextState = state.connected(
      selfReportedAddress = payload.myAddress,
      services = payload.services,
      version = payload.version,
      userAgent = payload.userAgent,
      height = payload.height
    )
    context.become(onVerackPayloadReceived(nextState) orElse onSomethingUnexpected)
  }

  def onVerackPayloadReceived(state: PeerState.Connected): Actor.Receive = onMessageReceived[protocol.VerackPayload] { payload =>
    log.info(s"Acknowledged connection to ${state.address}")
    sendMessage(protocol.GetHeadersPayload(state.network.genesisHash))
    val nextState = state.acknowledged
    context.become(onHeadersReceive(nextState) orElse onSomethingUnexpected)
  }

  def onHeadersReceive(state: PeerState.Acknowledged): Actor.Receive = onMessageReceived[protocol.HeadersPayload] { payload =>
    blockHeaderCount += payload.count

    log.info(s"downloaded: $blockHeaderCount headers")

    if (blockHeaderCount < state.height) {
      val headersSent = payload.headers.takeRight(10)
      log.info(headersSent.map(x => x.hash).mkString("; "))
      sendMessage(protocol.GetHeadersPayload(headersSent.map(_.hash)))
      log.info(s"last: ${payload.headers.last.hash.toString}, ${payload.headers.last} ${ByteString(payload.headers.last.hash.toString)}")
      context.become(onHeadersReceive(state) orElse onSomethingUnexpected)
    }

    else {
//      log.info(s"last: ${payload.headers.reverse.take(10).map((x: BlockHeader) => Hash.toString(x.hash))}")
      log.info(s"last: ${payload.headers.last.hash}, ${payload.headers.last}")
    }
  }

  def onFailedConnection: Actor.Receive = { case CommandFailed(_: Connect) =>
    log.error(s"Can not connect to $remote")
  }

  def onSomethingUnexpected: Actor.Receive = { case e =>
    log.error(e.toString)
  }

  def onMessageReceived[A <: Payload : Codec](f: A => Unit): Actor.Receive = { case Received(blob) =>
    // TODO: remove buffer
    buffer.append(blob)
    protocol.Message.decode[A](buffer.result()) match {
      case Successful(DecodeResult(message, _)) =>
        buffer.clear()
        log.info(s"Received ${message.payload.command} message")
        f(message.payload)
      case Failure(e) =>
    }
  }

  def sendMessage[A <: protocol.Payload : Codec](payload: A) = {
    val message = protocol.Message(network, payload)
    log.info(s"Sending $payload on $network")
    for {
      bits <- Codec.encode(message)
    } yield sender ! Write(bits)
  }

  implicit def bitVectorToByteString(bits: BitVector): ByteString = ByteString(bits.toByteArray)
}

object Peer {
  def props(remote: InetSocketAddress, network: Network) = Props(classOf[Peer], remote, network)
}
