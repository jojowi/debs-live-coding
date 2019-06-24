package chat

import rescala._

import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.Dynamic.global
import scala.scalajs.js.JSConverters._

class Frontend {
  private object ui {
    var chatlog: Dynamic = _
    var messageField: Dynamic = _
    var sendButton: Dynamic = _
  }
  val message = Evt[String]

  def setMessages(messages: Seq[String]) = global $ { () =>
    ui.chatlog.empty()

    ui.chatlog append
      (messages.reverseIterator map { case message =>
        global $("""<li/>""") text message
      }).toJSArray

    val last = ui.chatlog.children() get -1
    if (!(js isUndefined last))
      last.scrollIntoView(false)
  }

  global $ { () =>
    ui.chatlog = global $ "#chatlog"
    ui.messageField = global $ "#message"
    ui.sendButton = global $ "#send"

    ui.sendButton on ("click",
      { () =>
        message fire ui.messageField.`val`().toString
      }
    )
  }
}
