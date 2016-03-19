package one.eliot.machinomics.net

import java.net.InetSocketAddress
import akka.actor.{ActorLogging, ActorRef, Actor, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import one.eliot.machinomics.net.protocol.VersionPayload
import scodec._

class Peer(remote: InetSocketAddress, node: ActorRef, network: Network) extends Actor with ActorLogging {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  override def receive = {
    case CommandFailed(_: Connect) => {
      log.error(s"Can not connect to $remote")
    }
    case c @ Connected(r, l) => {
      log.info(s"Connected to $r")
      sender ! Register(self)
      println("Registered")
      val versionMessage = VersionPayload(network, remote.getAddress)
      sendMessage(versionMessage)
      context.become {
        case Received(data) => {
          println(data)
        }
        case e => println(s"GOT $e")
      }
    }
    case e => println(s"GOT $e")
  }

  def sendMessage[A: Codec](message: A) = {
    Codec.encode(message).map { b =>
      println(b.toByteVector)
    }
    val t = for {
      bits <- Codec.encode(message)
      bytes = bits.toByteArray
    } yield sender ! Write(ByteString(bytes))
  }
}

object Peer {
  def props(remote: InetSocketAddress, node: ActorRef, network: Network) = Props(classOf[Peer], remote, node, network)
}
