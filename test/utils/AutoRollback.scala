package utils

import org.specs2.mutable.After

trait AutoRollback extends After {

  def name = "default"

  implicit val connection = DB.get(name).getConnection(autocommit = false)

  def after: Any =
    try
      connection.rollback()
    finally
      connection.close()
}
