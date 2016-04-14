package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import one.eliot.machinomics.blockchain.DoubleHash
import one.eliot.machinomics.net.protocol._
import scodec.Attempt.{Failure, Successful}
import scodec._
import scodec.bits._

class Peer(remote: InetSocketAddress, node: ActorRef, network: Network) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  var services:   Services = Services()
  var version:    ProtocolVersion.Value = network.protocolVersion
  var selfReportedAddress:    NetworkAddress = NetworkAddress(remote, network)
  var userAgent:  String = ""
  var height:     Int = 0
  var acked:      Boolean = false

  var receivedNumber = 0

  var buffer = ByteString.newBuilder

  var blockHeaderCount = 0

  IO(Tcp) ! Connect(remote)

  override def receive = onConnected orElse onFailedConnection orElse onSomethingUnexpected

  def onConnected: Actor.Receive = { case c @ Connected(r, l) =>
    log.info(s"Connected to $r")
    sender ! Register(self)
    sendMessage(protocol.VersionPayload(network, remote.getAddress))
    context.become(onVersionPayloadReceived orElse onSomethingUnexpected)
  }

  def onVersionPayloadReceived: Actor.Receive = onMessageReceived[protocol.VersionPayload] { payload =>
    services = payload.services
    version  = payload.version
    selfReportedAddress  = payload.myAddress
    userAgent = payload.userAgent
    height = payload.height
    sendMessage(protocol.VerackPayload())
    context.become(onVerackPayloadReceived orElse onSomethingUnexpected)
  }

  def onVerackPayloadReceived: Actor.Receive = onMessageReceived[protocol.VerackPayload] { payload =>
    acked = true
    println(height)

    //TODO: remove hardcoded genesis block
//    val genesisHashString = "000000000933ea01ad0ee984209779baaec3ced90fa3f408719526f8d77f4943"
    val genesisHash = DoubleHash(hex"0000000005bdbddb59a3cd33b69db94fa67669c41d9d32751512b5d7b68c71cf".toArray)
//    val genesisHash = genesisHashString
//      .replaceAll("[^0-9A-Fa-f]", "")
//      .sliding(2, 2)
//      .toArray
//      .map(Integer.parseInt(_, 16).toByte)

    sendMessage(protocol.GetHeadersPayload(List(genesisHash)))

    context.become(onHeadersReceive orElse onSomethingUnexpected)
  }

  def onHeadersReceive: Actor.Receive = onMessageReceived[protocol.HeadersPayload] { payload =>
    blockHeaderCount += payload.count

    log.info(s"downloaded: $blockHeaderCount headers")

    if (blockHeaderCount < height) {
      val headersSent = payload.headers.takeRight(10)
      log.info(headersSent.map(x => x.hash).mkString("; "))
      sendMessage(protocol.GetHeadersPayload(headersSent.map(_.hash)))
      log.info(s"last: ${payload.headers.last.hash.toString}, ${payload.headers.last} ${ByteString(payload.headers.last.hash.toString)}")
      context.become(onHeadersReceive orElse onSomethingUnexpected)
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
    log.info(e.toString)
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
  def props(remote: InetSocketAddress, node: ActorRef, network: Network) = Props(classOf[Peer], remote, node, network)
}
