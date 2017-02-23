//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.bugs

import akka.http.scaladsl.model.ContentTypes.`text/html(UTF-8)`
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives._
import com.github.mauricio.async.db.RowData
import org.joda.time.LocalDateTime
import ru.rknrl.bugs.Utils._

object Client {

  case class Bug(id: Long, time: LocalDateTime, app: String, bug: String, text: String)

  def makeLink(href: String, content: String) = "<a href=\"" + href + "\">" + content + "</a>"

  def sameLabel(same: Long) = if (same > 1) same + " same" else ""

  def tr(content: String) = "<tr>" + content + "</tr>"

  def td(content: String) = "<td>" + content + "</td>"

  def bugParser(rowData: RowData): Bug =
    Bug(
      id = rowData("id").asInstanceOf[Long],
      time = rowData("time").asInstanceOf[LocalDateTime],
      app = rowData("app").asInstanceOf[String],
      bug = rowData("bug").asInstanceOf[String],
      text = if (rowData.size > 4) rowData("text").asInstanceOf[String] else ""
    )

  def bugRow(bug: Bug, href: String, same: Long) =
    tr(
      td(makeLink(href, bug.id.toString)) +
        td(makeLink(href, bug.time.toString)) +
        td(makeLink(href, bug.bug)) +
        td(makeLink(href, sameLabel(same)))
    )

  def bugLink(bugId: Long) = "/bug/" + bugId

  def sameBugsLink(bugId: Long, startDate: LocalDateTime, endDate: LocalDateTime) =
    "?day=" + startDate.getDayOfMonth +
      "&month=" + startDate.getMonthOfYear +
      "&year=" + startDate.getYear +
      "&toDay=" + endDate.getDayOfMonth +
      "&toMonth=" + endDate.getMonthOfYear +
      "&toYear=" + endDate.getYear +
      "&same=" + bugId

  def makeBugRow(groupByBug: (String, IndexedSeq[Bug]), startDate: LocalDateTime, endDate: LocalDateTime): String = {
    val (bug, sameBugs) = groupByBug

    val head = sameBugs.head

    val href = if (sameBugs.size > 1)
      sameBugsLink(head.id, startDate, endDate)
    else
      bugLink(head.id)

    bugRow(head, href, sameBugs.size)
  }

  def makeBugsTable(groupByBug: Map[String, IndexedSeq[Bug]], startDate: LocalDateTime, endDate: LocalDateTime): String =
    groupByBug.map(x ⇒ makeBugRow(x, startDate, endDate)).mkString

  def makeAppBugsTable(groupByApp: Map[String, IndexedSeq[Bug]], startDate: LocalDateTime, endDate: LocalDateTime): String = {
    val groupByBugs = groupByApp.map { case (app, bugs) ⇒ (app, bugs.groupBy(_.bug)) }

    groupByBugs.map { case (app, bugs) ⇒
      "<h3>" + app + "</h3>" +
        "<p>" + bugs.values.map(_.size).sum + " bugs, " + bugs.size + " different </p>" +
        "<table>" + makeBugsTable(bugs, startDate, endDate) + "</table>"
    }.mkString
  }

  def makeSameBugsTable(bugs: IndexedSeq[Bug], startDate: LocalDateTime, endDate: LocalDateTime): String =
    "<h3>Same bugs</h3>" +
      "<p>" + bugs.size + " same bugs</p>" +
      "<table>" + bugs.map(bug ⇒ bugRow(bug, bugLink(bug.id), same = 1)).mkString + "</table>"

  def main(args: Array[String]): Unit =
    app(args(0), (config, connection, executionContext) ⇒ {
      implicit val pool = connection
      implicit val ctx = executionContext

      def htmlHeader = "<link rel=\"stylesheet\" type=\"text/css\" href=\"http://" + config.host + "/bugs.css\" />"

      def makeReport(startDate: LocalDateTime, endDate: LocalDateTime) = {
        val f = Db.getAll("SELECT id, time, app, bug from bugs WHERE time > ? AND time < ? ORDER BY time DESC", Seq(startDate, endDate), bugParser)
        onSuccess(f) { bugs ⇒
          complete(HttpEntity(`text/html(UTF-8)`, htmlHeader + "<h1>Bugs Server</h1>" + makeAppBugsTable(bugs.groupBy(_.app), startDate, endDate)))
        }
      }

      def makeSamePage(id: Long, startDate: LocalDateTime, endDate: LocalDateTime) = {
        val f = Db.getAll("SELECT id, time, app, bug FROM bugs WHERE bug = (SELECT bug FROM bugs WHERE id = ?) AND app = (SELECT app FROM bugs WHERE id = ?) AND time > ? AND time < ?", Seq(id, id, startDate, endDate), bugParser)
        onSuccess(f) { bugs ⇒
          complete(HttpEntity(`text/html(UTF-8)`, htmlHeader + "<h1>Same Bugs</h1>" + makeSameBugsTable(bugs, startDate, endDate)))
        }
      }

      def makeBugPage(id: Long) = {
        val f = Db.getOne("SELECT id, time, app, bug, text from bugs WHERE id = ?", Seq(id), bugParser)
        onSuccess(f) { bug ⇒
          complete(HttpEntity(`text/html(UTF-8)`, htmlHeader + s"<h1>Bug $id</h1><p>${bug.app}</p><p>${bug.time}</p><p>${bug.bug}</p><pre>${bug.text}</pre>"))
        }
      }

      get {
        pathSingleSlash {
          parameters('year.?, 'month.?, 'day.?, 'toYear.?, 'toMonth.?, 'toDay.?, 'same.?) { (fromYear, fromMonth, fromDay, toYear, toMonth, toDay, same) ⇒
            val now = LocalDateTime.now

            val startDate = new LocalDateTime(
              fromYear.map(_.toInt).getOrElse(now.getYear),
              fromMonth.map(_.toInt).getOrElse(now.getMonthOfYear),
              fromDay.map(_.toInt).getOrElse(now.getDayOfMonth),
              0,
              0
            )

            val tomorrow = startDate.plusHours(24)

            val endDate = new LocalDateTime(
              toYear.map(_.toInt).getOrElse(tomorrow.getYear),
              toMonth.map(_.toInt).getOrElse(tomorrow.getMonthOfYear),
              toDay.map(_.toInt).getOrElse(tomorrow.getDayOfMonth),
              0,
              0
            )

            if (same.isDefined)
              makeSamePage(same.get.toInt, startDate, endDate)
            else
              makeReport(startDate, endDate)
          }
        } ~
          path("bug" / LongNumber) { id ⇒
            makeBugPage(id)
          }
      }
    })
}
