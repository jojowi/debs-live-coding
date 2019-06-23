package chat

import rescala.Evt

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.JSConverters._
import rescala._

class Frontend {
  private object ui {
    var chatlog: Dynamic = _
    var message: Dynamic = _
    var send: Dynamic = _
    var create: Dynamic = _
    var channelName: Dynamic = _
    var table: Dynamic = _
    var main: Dynamic = _
    var channel: Dynamic = _
    var channelTitle: Dynamic = _
    var backToMain: Dynamic = _
  }
  val message = Evt[Message]
  val channel = Evt[String]
  val currentChannel = Var(new Channel("", -1))

  private val $ = global.$

  def addMessages(messages: Seq[String]): Unit = $ { () =>
    ui.chatlog append
      (messages.reverseIterator map { case message =>
        $("""<li/>""") text message
      }).toJSArray

    val last = ui.chatlog.children() get -1
    if (!(js isUndefined last))
      last.scrollIntoView(false)
  }

  def setChannels(channels: Seq[Channel]): Unit = $ { () =>
    ui.table.empty();
    ui.table.append("<tr><th>ID</th><th>Title</th></tr>");
    channels.reverseIterator foreach { channel =>
      ui.table append (
        $("""<tr/>""") append (
          $("""<td/>""") text channel.id
          ) append (
          $("""<td/>""") text channel.name
          ) append (
          $("""<button class="btn btn-default"/>""") click { event: Dynamic =>
            gotoChannel(channel)
          } text "Enter"
          )
        )
    }
  }

  def setMessages(messages: Seq[String]): Unit = $ { () =>
    ui.chatlog.empty()
    addMessages(messages)
  }

  def gotoChannel(channel: Channel): Unit = $ { () =>
    currentChannel.set(channel)
    ui.main.hide()
    ui.channel.show()
    ui.channelTitle text channel.name
  }

  $ { () =>
    ui.chatlog = global $ "#chatlog"
    ui.message = global $ "#message"
    ui.send = global $ "#send"
    ui.create = global $ "#createChannel"
    ui.channelName = global $ "#channelName"
    ui.table = global $ "#channelsTable"
    ui.channel = global $ "#channel"
    ui.main = global $ "#main"
    ui.channelTitle = global $ "#channelTitle"
    ui.backToMain = global $ "#backToMain"
    ui.channel.hide()


    ui.backToMain on ("click",
      { () =>
        ui.channel.hide()
        ui.main.show()
      }
    )

    ui.create on ("click",
      { () =>
        channel fire ui.channelName.`val`().toString
      }
    )

    ui.channelName on ("keyup", { event: Dynamic =>
      if (event.keyCode == 13) {
        event.preventDefault()
        ui.create trigger "click"
      }
    })

    ui.send on ("click",
      { () =>
        println("send")
        message fire new Message(currentChannel.now.id, ui.message.`val`().toString)
      }
    )

    ui.message on ("keyup", { event: Dynamic =>
      if (event.keyCode == 13) {
        event.preventDefault()
        ui.send trigger "click"
      }
    })
  }
}
