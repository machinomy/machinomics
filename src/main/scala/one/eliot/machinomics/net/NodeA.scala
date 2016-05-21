package one.eliot.machinomics.net

import akka.actor._
import one.eliot.machinomics.blockchain.Block
import one.eliot.machinomics.store.MapsDbBlockStore

class NodeA(network: Network) extends Actor with ActorLogging {
  override def receive: Receive = expectStart(NodeState.Initial.forNetwork(network))
  val db = new MapsDbBlockStore()

  def expectStart(state: NodeState.Initial): Receive = { case NodeA.Start() =>
    val herd = context.actorOf(Herd.props(network))
    herd ! Herd.Connect(state.peersCount)
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
    /*state.herd ! Herd.GetHeaders()
    context.become(receivingHeaders(state))*/
  }

  def receivingHeaders(state: NodeState.Working): Receive = {
    case Herd.GotHeaders(headers) =>
      for (h <- headers) {
        db.put(Block(h, Array.empty))
      }
      println(s"!!!!!! GOTHEADERS: ${headers.size}")
//    case PeerConnectionA.GotNoMoreHeaders() =>
//      println("!!!!!! NOMOREHEADERS")
//      context.unbecome()
  }

  override def postStop(): Unit = {
    db.close()
  }
}

object NodeA {
  def props(network: Network): Props = Props(classOf[NodeA], network)

  sealed trait Message
  case class Start() extends Message
}

