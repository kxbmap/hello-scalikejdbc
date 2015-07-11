package utils

import org.specs2.mutable.After

trait AutoRollback extends After {

  def name = "default"

  implicit val session = DB.get(name).getSession(autoCommit = false)

  def after: Any = {
    val conn = session.configuration.connectionProvider().acquire()
    try
      conn.rollback()
    finally
      conn.close()
  }
}
