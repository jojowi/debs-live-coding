package chat;

import loci._
import rescala._
import loci.transmitter.rescala._
import loci.serializer.upickle._


@multitier
object Application {
  @peer type Peer
  @peer type Server <: Peer { type Tie <: Multiple[Client] }
  @peer type Client <: Peer { type Tie <: Single[Server] }

  val ui: Local[Frontend] on Client

  val messages = on[Server] { Var(List[String]()) }

  val message = on[Client] { ui.message }

  def main(): Unit on Peer =
    (on[Server] {
      message.asLocalFromAllSeq observe { case (index,message) =>
        messages.transform(messages => message :: messages)
      }
    }
    and on[Client] {
      messages.asLocal observe { list =>
        ui.setMessages(list)
      }
    })


}
