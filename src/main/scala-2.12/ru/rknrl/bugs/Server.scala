//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.bugs

import akka.event.slf4j.Logger
import akka.http.scaladsl.model.ContentTypes.`text/xml(UTF-8)`
import akka.http.scaladsl.model.{HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Directives._
import ru.rknrl.bugs.Utils._

object Server {

  def getNextParam(lines: Iterator[String], paramName: String): String = {
    val arr = if (lines.hasNext) lines.next.split("=") else Array.empty
    if (arr.length == 2 && arr.head == paramName) arr.last else ""
  }

  val crossdomain = "<?xml version=\"1.0\"?>\n" +
    "<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">\n" +
    "<cross-domain-policy>\n" +
    "    <site-control permitted-cross-domain-policies=\"master-only\"/>\n" +
    "    <allow-access-from domain=\"*\" to-ports=\"*\"/>\n" +
    "</cross-domain-policy>"

  val logger = Logger("Server")

  def main(args: Array[String]): Unit =
    app(args(0), (config, connection, executionContext) ⇒
      get {
        path("crossdomain.xml") {
          complete(HttpEntity(`text/xml(UTF-8)`, crossdomain))
        }
      } ~
        post {
          path("bug") {
            entity(as[String]) { string ⇒
              if (logger.isDebugEnabled) logger.debug("Receive bug")
              val lines = string.lines
              val secret = getNextParam(lines, "secret")
              val app = getNextParam(lines, "app")
              val bug = getNextParam(lines, "bug")

              if (secret == config.secret) {
                connection.sendPreparedStatement("INSERT INTO bugs (time, app, bug, text) VALUES (NOW(), ?, ?, ?)", Seq(app, bug, string))
                complete(StatusCodes.OK)
              } else {
                if (logger.isDebugEnabled) logger.debug("Wrong secret")
                complete(StatusCodes.BadRequest)
              }
            }
          }
        }
    )
}

