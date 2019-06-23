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
    var messageField: Dynamic = _
    var sendButton: Dynamic = _
  }
  val message = Evt[String]

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

  def setMessages(messages: Seq[String]): Unit = $ { () =>
    ui.chatlog.empty()
    addMessages(messages)
  }

  $ { () =>
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
