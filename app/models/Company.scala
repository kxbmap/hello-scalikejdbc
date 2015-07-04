package models

import db.Tables
import java.sql.Connection
import org.joda.time.DateTime
import org.jooq.impl.DSL
import org.jooq.{Condition, Record, RecordMapper}
import scala.collection.JavaConverters._

case class Company(
    id: Long,
    name: String,
    url: Option[String] = None,
    createdAt: DateTime,
    deletedAt: Option[DateTime] = None) {

  def save()(implicit conn: Connection): Company = Company.save(this)

  def destroy()(implicit conn: Connection): Unit = Company.destroy(id)
}

object Company {

  def apply(c: db.tables.Company): RecordMapper[Record, Company] = new RecordMapper[Record, Company] {
    def map(record: Record): Company = Company(
      id = record.getValue(c.ID),
      name = record.getValue(c.NAME),
      url = Option(record.getValue(c.URL)),
      createdAt = new DateTime(record.getValue(c.CREATED_AT)),
      deletedAt = Option(record.getValue(c.DELETED_AT)).map(new DateTime(_))
    )
  }

  def opt(c: db.tables.Company): RecordMapper[Record, Option[Company]] = new RecordMapper[Record, Option[Company]] {
    val crm = Company(c)
    def map(record: Record): Option[Company] = Option(record.getValue(c.ID)).map(_ => crm.map(record))
  }

  val c = Tables.COMPANY.as("c")
  private val isNotDeleted = c.DELETED_AT.isNull

  def find(id: Long)(implicit conn: Connection): Option[Company] = {
    Option(DSL.using(conn).selectFrom(c).where(c.ID.equal(id).and(isNotDeleted)).fetchOne(Company(c)))
  }

  def findAll()(implicit conn: Connection): List[Company] = {
    DSL.using(conn)
      .selectFrom(c)
      .where(isNotDeleted)
      .orderBy(c.ID)
      .fetch(Company(c)).asScala.toList
  }

  def countAll()(implicit conn: Connection): Int = {
    DSL.using(conn).selectCount().from(c).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: Condition)(implicit conn: Connection): List[Company] = {
    DSL.using(conn)
      .selectFrom(c)
      .where(where.and(isNotDeleted))
      .orderBy(c.ID)
      .fetch(Company(c)).asScala.toList
  }

  def countBy(where: Condition)(implicit conn: Connection): Int = {
    DSL.using(conn).selectCount().from(c).where(where.and(isNotDeleted)).fetchOne().value1()
  }

  def create(name: String, url: Option[String] = None, createdAt: DateTime = DateTime.now)(implicit conn: Connection): Company = {
    val c = Tables.COMPANY

    val id = DSL.using(conn)
      .insertInto(c, c.NAME, c.URL, c.CREATED_AT)
      .values(name, url.orNull, createdAt)
      .returning(c.ID)
      .fetchOne().getId

    Company(id = id, name = name, url = url, createdAt = createdAt)
  }

  def save(m: Company)(implicit conn: Connection): Company = {
    DSL.using(conn)
      .update(c)
      .set(c.NAME, m.name)
      .set(c.URL, m.url.orNull)
      .where(c.ID.equal(m.id).and(isNotDeleted))
      .execute()
    m
  }

  def destroy(id: Long)(implicit conn: Connection): Unit = {
    DSL.using(conn)
      .update(c)
      .set(c.DELETED_AT, DateTime.now)
      .where(c.ID.equal(id).and(isNotDeleted))
      .execute()
  }

}
