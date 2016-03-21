package one.eliot.machinomics.net

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import one.eliot.machinomics.net.protocol.{Message, Payload, VersionPayload}
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
      sendMessage(VersionPayload(network, remote.getAddress))
      context.become {
        case Received(data) => {
          println("Received")
          println(data)
        }
        case e => println(e)
      }
    }
    case e => println(e)
  }

  def sendMessage[A <: Payload : Codec](payload: A) = {
    val message = Message(network, payload)
    Codec.encode(message).map { b =>
      println(b.toByteVector.toArray.toList)
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
