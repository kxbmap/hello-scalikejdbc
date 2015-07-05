package utils

import com.typesafe.config.ConfigFactory
import play.api.db.{Database, Databases}
import scala.collection.JavaConversions._
import scala.collection.concurrent.TrieMap

object DB {

  private lazy val databases = {
    val dbs = new TrieMap[String, Database]()
    sys.addShutdownHook {
      dbs.values.foreach(_.shutdown())
    }
    dbs
  }

  def get(name: String): Database = databases.getOrElseUpdate(name, {
    val config = ConfigFactory.load().getConfig(s"db.$name")
    Databases(
      driver = config.getString("driver"),
      url = config.getString("url"),
      name = name,
      config = config.root().unwrapped().toMap
    )
  })

}
