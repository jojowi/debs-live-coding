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
    ui.message = global $ "#message"
    ui.send = global $ "#send"

    ui.send on ("click",
      { () =>
        message fire ui.message.`val`().toString
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
