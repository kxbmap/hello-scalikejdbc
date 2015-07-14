package utils

import com.github.kxbmap.jooqs.SimpleConverter
import java.sql.Timestamp
import org.joda.time.DateTime

@SerialVersionUID(1L)
class DateTimeConverter extends SimpleConverter[Timestamp, DateTime] {

  def fromDB(value: Timestamp): DateTime = new DateTime(value.getTime)

  def toDB(value: DateTime): Timestamp = new Timestamp(value.getMillis)
}
