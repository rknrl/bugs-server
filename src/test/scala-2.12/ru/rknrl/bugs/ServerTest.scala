//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.bugs

import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.{Matchers, WordSpec}

class ServerTest extends WordSpec with Matchers with ScalatestRouteTest {
  "The service" should {
    "return a greeting for GET requests" in {
//      Post("/bug", "Content") ~> Server.route ~> check {
//        responseAs[String] shouldEqual "<h1>Content</h1>"
//      }
    }
  }
}
