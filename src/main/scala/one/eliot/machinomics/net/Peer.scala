package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}
import akka.util.{ByteString, ByteStringBuilder}
import one.eliot.machinomics.net.protocol._
import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.bits._

class Peer() extends Actor with ActorLogging {

  import Tcp._
  import context.system

  var blockHeaderCount = 0

  override def receive = onConnect

  def onConnect: Receive = { case Peer.Connect(remote, network) =>
    IO(Tcp) ! Connect(remote)
    val emptyState = PeerState.Empty(network, NetworkAddress(remote, network))
    context.become(onConnected(emptyState) orElse onFailedConnection(emptyState) orElse onSomethingUnexpected)
  }

  def onConnected(state: PeerState.Empty): Receive = { case c @ Connected(r, l) =>
    log.info(s"Connected to $r")
    sender ! Register(self)
    sendMessage(state.network, protocol.VersionPayload(state.network, state.address))
    become(onVersionPayloadReceived, state.initial)
  }

  def onVersionPayloadReceived(state: PeerState.Initial): Receive = onMessageReceived[protocol.VersionPayload] { payload =>
    log.info(s"Done version handshake with ${state.address}")
    sendMessage(state.network, protocol.VerackPayload())
    become(onVerackPayloadReceived, state.connected(
      selfReportedAddress = payload.myAddress,
      services = payload.services,
      version = payload.version,
      userAgent = payload.userAgent,
      height = payload.height
    ))
  }

  def onVerackPayloadReceived(state: PeerState.Connected): Receive = onMessageReceived[protocol.VerackPayload] { payload =>
    log.info(s"Acknowledged connection to ${state.address}")
    sendMessage(state.network, protocol.GetHeadersPayload(state.network.genesisHash))
    become(onHeadersReceive, state.acknowledged)
  }

  def onHeadersReceive(state: PeerState.Acknowledged): Receive = onMessageReceived[protocol.HeadersPayload] { payload =>
    blockHeaderCount += payload.count

    log.info(s"downloaded: $blockHeaderCount headers")

    if (blockHeaderCount < state.height) {
      val headersSent = payload.headers.takeRight(10)
      log.info(headersSent.map(x => x.hash).mkString("; "))
      sendMessage(state.network, protocol.GetHeadersPayload(headersSent.map(_.hash)))
      log.info(s"last: ${payload.headers.last.hash.toString}, ${payload.headers.last} ${ByteString(payload.headers.last.hash.toString)}")
      context.become(onHeadersReceive(state) orElse onSomethingUnexpected)
    }

    else {
//      log.info(s"last: ${payload.headers.reverse.take(10).map((x: BlockHeader) => Hash.toString(x.hash))}")
      log.info(s"last: ${payload.headers.last.hash}, ${payload.headers.last}")
    }
  }

  def onFailedConnection(state: PeerState.Empty): Receive = { case CommandFailed(_: Connect) =>
    log.error(s"Can not connect to ${state.address}")
  }

  def onSomethingUnexpected: Receive = { case e =>
    log.error(e.toString)
  }

  def become[S](behavior: S => Receive, nextState: S) = context.become(behavior(nextState) orElse onSomethingUnexpected)

  def onMessageReceived[A <: Payload : Codec](f: A => Unit, buffer: ByteStringBuilder = ByteString.newBuilder): Receive = { case Received(blob) =>
    // TODO: remove buffer
    buffer.append(blob)
    protocol.Message.decode[A](buffer.result()) match {
      case Successful(DecodeResult(message, _)) =>
        buffer.clear()
        log.info(s"Received ${message.payload.command} message")
        f(message.payload)
      case Failure(e) =>
        context.become(onMessageReceived(f, buffer) orElse onSomethingUnexpected)
    }
  }

  def sendMessage[A <: protocol.Payload : Codec](network: Network, payload: A) = {
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

  sealed trait Message
  case class Connect(remote: InetSocketAddress, network: Network) extends Message
}
