package chat

import loci._
import rescala._
import upickle.default._
import loci.transmitter.rescala._
import loci.serializer.upickle._
import loci.transmitter.IdenticallyTransmittable

final case class Channel(name: String, id: Int)
object Channel {
  implicit val channelTransmittable: IdenticallyTransmittable[Channel] = IdenticallyTransmittable()
  implicit val channelPickler: ReadWriter[Channel] = macroRW
}
final case class Message(channel: Int, message: String)
object Message {
  implicit val messageTransmittable: IdenticallyTransmittable[Message] = IdenticallyTransmittable()
  implicit val messagePickler: ReadWriter[Message] = macroRW
}


@multitier
object Application {
  @peer type Peer
  @peer type Server <: Peer { type Tie <: Multiple[Client] }
  @peer type Client <: Peer { type Tie <: Single[Server] }

  val ui: Local[Frontend] on Client
  var handler: Local[Observe] on Client = _

  var messages = on[Server] { implicit! => Var[Map[Int,Seq[String]]](Map.empty) }
  val channels = on[Server] { implicit! => Var(List[Channel]()) }

  val message = on[Client] { implicit! => ui.message }
  val channel = on[Client] { implicit! => ui.channel }

  var nextid = on[Server] { implicit! => 0 }

  def main(): Unit on Peer =
    (on[Server] { implicit ! =>
      message.asLocalFromAllSeq observe { case (remote, message) =>
        messages.transform(messages => {
          val channelMessages = messages.getOrElse(message.channel, Seq.empty)
          val newmessages = Seq(message.message) ++ channelMessages
          messages + (message.channel -> newmessages)
        })
      }
      channel.asLocalFromAllSeq observe { case (remote, name) =>
        val channel = new Channel(name, nextid)
        nextid = nextid + 1
        channels.transform(channels => channel :: channels)
      }
    }
      and on[Client] { implicit! =>
      channels.asLocal observe { list =>
        ui.setChannels(list)
      }

      ui.currentChannel observe { channel =>
        if (channel.id != -1) {
          if (handler != null) handler.remove()
          handler = messages.asLocal observe { map =>
            ui.setMessages(map.getOrElse(channel.id, Seq.empty))
          }
        }
      }
    })


}
