package utils

import java.sql.Timestamp
import org.joda.time.DateTime
import org.jooq.Converter

class DateTimeConverter extends Converter[Timestamp, DateTime] {
  def from(databaseObject: Timestamp): DateTime = databaseObject match {
    case null => null
    case ts   => new DateTime(ts.getTime)
  }

  def to(userObject: DateTime): Timestamp = userObject match {
    case null => null
    case dt   => new Timestamp(dt.getMillis)
  }

  def fromType(): Class[Timestamp] = classOf[Timestamp]

  def toType: Class[DateTime] = classOf[DateTime]
}
