//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.bugs

import akka.event.slf4j.Logger
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import ru.rknrl.bugs.Utils._

object Server {

  def getNextParam(lines: Iterator[String], paramName: String): String = {
    val arr = if (lines.hasNext) lines.next.split("=") else Array.empty
    if (arr.length == 2 && arr.head == paramName) arr.last else ""
  }

  val logger = Logger("Server")

  def main(args: Array[String]): Unit =
    app(args(0), (config, connection, executionContext) ⇒
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

