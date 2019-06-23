package chat

import util._
import loci._
import loci.communicator.ws.akka.{WS, WebSocketListener}
import akka.http.scaladsl.model.ContentType
import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.HttpCharsets._
import akka.http.scaladsl.server.Directives._
import loci.contexts.Pooled.Implicits.global

import scala.scalajs.js

object Server extends App {
  val webSocket = WebSocketListener()

  val route =
    get {
      pathSingleSlash {
        webSocket ~
        getFromResource("index.xhtml", ContentType(`application/xhtml+xml`, `UTF-8`))
      } ~
      path("app.js") {
        getFromResource("chatjs-fastopt.js")
      } ~
      pathPrefix("lib") {
        getFromResourceDirectory("META-INF/resources/webjars")
      }
    }

  HttpServer start (route, "localhost", 8080) foreach { server =>
    val runtime = multitier start new Instance[Application.Server](
      listen[Application.Client] { webSocket })

    runtime.terminated onComplete { _ =>
      server.stop
    }
  }
}

object Client extends js.JSApp {
  def main() = multitier start new Instance[Application.Client](
      connect[Application.Server] { WS("ws://localhost:8080") }) {
    val ui = new Frontend
  }
}