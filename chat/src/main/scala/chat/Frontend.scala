package chat

import rescala._

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.JSConverters._

class Frontend {
  private object ui {
    var mainView: Dynamic = _
    var channelTable: Dynamic = _
    var channelNameField: Dynamic = _
    var createButton: Dynamic = _

    var channelView: Dynamic = _
    var backToMainButton: Dynamic = _
    var channelTitle: Dynamic = _
    var messageField: Dynamic = _
    var sendButton: Dynamic = _
    var chatlog: Dynamic = _
  }
  val message = Evt[Message]
  val channel = Evt[String]
  val currentChannel = Var(new Channel("", -1))

  def setChannels(channels: Seq[Channel]): Unit = global $ { () =>
    ui.channelTable.empty();
    ui.channelTable.append("<tr><th>ID</th><th>Title</th></tr>");
    channels.reverseIterator foreach { channel =>
      ui.channelTable append (
        global $("<tr/>") append (
            global $("<td/>") text channel.id
          ) append (
            global $("<td/>") text channel.name
          ) append (
            global $("""<button class="btn btn-default"/>""") click { event: Dynamic =>
              gotoChannel(channel)
            } text "Enter"
          )
        )
    }
  }

  def setMessages(messages: Seq[String]): Unit = global $ { () =>
    ui.chatlog.empty()

    ui.chatlog append
      (messages.reverseIterator map { case message =>
        global $("<li/>") text message
      }).toJSArray

    val last = ui.chatlog.children() get -1
    if (!(js isUndefined last))
      last.scrollIntoView(false)
  }

  private def gotoChannel(channel: Channel): Unit = global $ { () =>
    currentChannel.set(channel)
    ui.mainView.hide()
    ui.channelView.show()
    ui.channelTitle text channel.name
  }

  global $ { () =>
    ui.chatlog = global $ "#chatlog"
    ui.messageField = global $ "#message"
    ui.sendButton = global $ "#send"
    ui.createButton = global $ "#createChannel"
    ui.channelNameField = global $ "#channelName"
    ui.channelTable = global $ "#channelsTable"
    ui.channelView = global $ "#channel"
    ui.mainView = global $ "#main"
    ui.channelTitle = global $ "#channelTitle"
    ui.backToMainButton = global $ "#backToMain"
    ui.channelView.hide()

    ui.backToMainButton on ("click",
      { () =>
        ui.channelView.hide()
        ui.mainView.show()
      }
    )

    ui.createButton on ("click",
      { () =>
        channel fire ui.channelNameField.`val`().toString
      }
    )

    ui.sendButton on ("click",
      { () =>
        message fire new Message(currentChannel.now.id, ui.messageField.`val`().toString)
      }
    )
  }
}