package one.eliot.machinomics.net

import akka.actor._

class Node(network: Network) extends Actor with ActorLogging {
  override def receive: Receive = expectStart(NodeState.Initial.forNetwork(network))

  def expectStart(state: NodeState.Initial): Receive = { case Node.Start() =>
    val herd = context.actorOf(Herd.props(network))
    herd ! Herd.Connect(10)
    val nextState = state.working(herd)
    context.become(onHerdDidConnect(nextState))
  }

  def onHerdDidConnect(state: NodeState.Working): Receive = { case Herd.DidConnect() =>
    log.info("Herd did connect")
    state.herd ! Herd.Handshake()
    context.become(onHerdDidHandshake(state))
  }

  def onHerdDidHandshake(state: NodeState.Working): Receive = { case Herd.DidHandshake() =>
    log.info("Herd did handshake")
    state.herd ! Herd.GetHeaders()
    context.become(receivingHeaders(state))
  }

  def receivingHeaders(state: NodeState.Working): Receive = {
    case Herd.GotHeaders(headers) =>
      println(s"!!!!!! GOTHEADERS: ${headers.size}")
//    case Peer.GotNoMoreHeaders() =>
//      println("!!!!!! NOMOREHEADERS")
//      context.unbecome()
  }
}

object Node {
  def props(network: Network): Props = Props(classOf[Node], network)

  sealed trait Message
  case class Start() extends Message
}

