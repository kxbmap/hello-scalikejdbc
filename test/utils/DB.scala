package utils

import com.github.kxbmap.jooq.db.Database
import com.typesafe.config.ConfigFactory
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
    Class.forName(config.getString("driver"))
    Database(
      url = config.getString("url"),
      user = config.getString("username"),
      password = config.getString("password")
    )
  })

}
