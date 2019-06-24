package chat;

import loci._
import loci.transmitter.rescala._
import loci.serializer.upickle._

import rescala._

@multitier
object Application {
  @peer type Server <: { type Tie <: Multiple[Client] }
  @peer type Client <: { type Tie <: Single[Server] }

  val ui: Local[Frontend] on Client

  val messages = on[Server] {
    (message.asLocalFromAllSeq map {
      case (_, message) => message
    }).list
  }

  val message = on[Client] { ui.message }

  def main(): Unit on Client =
    on[Client] {
      messages.asLocal observe { list =>
        ui.setMessages(list)
      }
    }
}
