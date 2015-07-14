package utils

import org.specs2.mutable.After

trait AutoRollback extends After {

  def name = "default"

  implicit val session = DB.get(name).getSession(autoCommit = false)

  def after: Any =
    try
      session.rollback()
    finally
      session.close()
}
