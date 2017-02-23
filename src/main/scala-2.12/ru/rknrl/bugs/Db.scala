//       ___       ___       ___       ___       ___
//      /\  \     /\__\     /\__\     /\  \     /\__\
//     /::\  \   /:/ _/_   /:| _|_   /::\  \   /:/  /
//    /::\:\__\ /::-"\__\ /::|/\__\ /::\:\__\ /:/__/
//    \;:::/  / \;:;-",-" \/|::/  / \;:::/  / \:\  \
//     |:\/__/   |:|  |     |:/  /   |:\/__/   \:\__\
//      \|__|     \|__|     \/__/     \|__|     \/__/

package ru.rknrl.bugs

import com.github.mauricio.async.db.{Configuration, Connection, QueryResult, RowData}
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.pool.PoolConfiguration

import scala.concurrent.{ExecutionContext, Future}

/**
  * Some utils for work with `com.github.mauricio.async.db`
  */
object Db {

  case class DbConfiguration(username: String,
                             host: String,
                             port: Int,
                             password: String,
                             database: String,
                             poolMaxObjects: Int,
                             poolMaxIdle: Long,
                             poolMaxQueueSize: Int) {
    def configuration = Configuration(
      username = username,
      host = host,
      port = port,
      password = Some(password),
      database = Some(database)
    )

    def poolConfiguration = PoolConfiguration(
      maxObjects = poolMaxObjects,
      maxIdle = poolMaxIdle,
      maxQueueSize = poolMaxQueueSize
    )
  }

  class NotFound extends Exception("Not found")

  def replaceOne(query: String, values: Seq[Any])(implicit c: Connection, executor: ExecutionContext): Future[Unit] =
    execute(query, values) flatMap {
      queryResult ⇒
        if (queryResult.rowsAffected != 1 && queryResult.rowsAffected != 2) // Операция REPLACE сначала удаляет а потом вставляет строку, поэтому rowsAffected == 2
          Future.failed(new IllegalArgumentException("Expected 1 rows affected, but get " + queryResult.rowsAffected + ", query: " + query))
        else
          Future.successful()
    }

  def updateOne(query: String, values: Seq[Any])(implicit c: Connection, executor: ExecutionContext): Future[Unit] =
    execute(query, values) flatMap {
      queryResult ⇒
        if (queryResult.rowsAffected != 1)
          Future.failed(new IllegalArgumentException("Expected 1 rows affected, but get " + queryResult.rowsAffected + ", query: " + query))
        else
          Future.successful()
    }

  def getOne[A](query: String, values: Seq[Any], parser: RowData ⇒ A)(implicit c: Connection, executor: ExecutionContext): Future[A] =
    getAll(query, values, parser) flatMap {
      rows ⇒
        if (rows.size > 1)
          Future.failed(new IllegalArgumentException("Expected 1 row, but get " + rows.size + ", query: " + query))
        else if (rows.isEmpty)
          Future.failed(new NotFound)
        else
          Future.successful(rows.head)
    }

  def getAll[A](query: String, values: Seq[Any], parser: RowData ⇒ A)(implicit c: Connection, executor: ExecutionContext): Future[IndexedSeq[A]] =
    execute(query, values) flatMap {
      queryResult ⇒
        queryResult.rows match {
          case Some(rows) ⇒ Future.successful(rows.map(parser))
          case None ⇒ Future.failed(new IllegalArgumentException("Get None"))
        }
    }

  def getOptionOne[A](query: String, values: Seq[Any], parser: RowData ⇒ A)(implicit c: Connection, executor: ExecutionContext): Future[Option[A]] =
    getAll(query, values, parser) flatMap { xs ⇒
      if (xs.size > 1)
        Future.failed(throw new IllegalArgumentException("Expected 0 or 1 rows, but get " + xs.size))
      else
        Future.successful(xs.headOption)
    }

  def execute(query: String, values: Seq[Any])(implicit c: Connection): Future[QueryResult] = {
    MySQLConnection.log.debug(query + "; " + values)
    if (values.nonEmpty)
      c.sendPreparedStatement(query, values)
    else
      c.sendQuery(query)
  }
}
