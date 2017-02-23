//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.bugs

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.github.mauricio.async.db.pool.ConnectionPool
import net.liftweb.json.{DefaultFormats, JsonParser}
import ru.rknrl.bugs.Db.DbConfiguration

import scala.concurrent.ExecutionContext
import scala.io.Source

object Utils {

  case class Config(host: String,
                    port: Int,
                    secret: String,
                    db: DbConfiguration)

  def app(configPath: String, route: (Config,  Connection, ExecutionContext) ⇒ Route): Unit = {
    val configString = Source.fromFile(configPath, "UTF-8").mkString
    implicit val formats = DefaultFormats
    val config = JsonParser.parse(configString).extract[Config]

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    val factory = new MySQLConnectionFactory(config.db.configuration)
    implicit val pool = new ConnectionPool(factory, config.db.poolConfiguration)

    val bindingFuture = Http().bindAndHandle(route(config, pool, executionContext), config.host, config.port)
    println(s"Server online at http://${config.host}:${config.port}/")

    sys.addShutdownHook {
      println("Shutdown..")
      bindingFuture
        .flatMap(_.unbind())
        .onComplete(_ ⇒ system.terminate())
    }
  }
}
