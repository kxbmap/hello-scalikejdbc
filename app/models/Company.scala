package models

import db.{Tables, tables}
import jooqs.DBSession
import jooqs.syntax._
import org.joda.time.DateTime
import org.jooq.{Condition, Record, RecordMapper}
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

case class Company(
    id: Long,
    name: String,
    url: Option[String] = None,
    createdAt: DateTime,
    deletedAt: Option[DateTime] = None) {

  def save()(implicit session: DBSession): Company = Company.save(this)

  def destroy()(implicit session: DBSession): Unit = Company.destroy(id)
}

object Company {

  def apply(c: tables.Company): RecordMapper[Record, Company] = new RecordMapper[Record, Company] {
    def map(record: Record): Company = Company(
      id = record.get(c.ID),
      name = record.get(c.NAME),
      url = record.getOpt(c.URL),
      createdAt = record.get(c.CREATED_AT),
      deletedAt = record.getOpt(c.DELETED_AT)
    )
  }

  def opt(c: tables.Company): RecordMapper[Record, Option[Company]] = new RecordMapper[Record, Option[Company]] {
    val crm = Company(c)
    def map(record: Record): Option[Company] = record.getOpt(c.ID).map(_ => crm.map(record))
  }

  val c = Tables.COMPANY.as("c")
  private val isNotDeleted = c.DELETED_AT.isNull

  def find(id: Long)(implicit session: DBSession): Option[Company] = {
    dsl.selectFrom(c).where(c.ID === id && isNotDeleted).fetchOptional(Company(c)).asScala
  }

  def findAll()(implicit session: DBSession): List[Company] = {
    dsl.selectFrom(c)
      .where(isNotDeleted)
      .orderBy(c.ID)
      .fetch(Company(c)).asScala.toList
  }

  def countAll()(implicit session: DBSession): Int = {
    dsl.selectCount().from(c).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: tables.Company => Condition)(implicit session: DBSession): List[Company] = {
    dsl.selectFrom(c)
      .where(where(c) && isNotDeleted)
      .orderBy(c.ID)
      .fetch(Company(c)).asScala.toList
  }

  def countBy(where: tables.Company => Condition)(implicit session: DBSession): Int = {
    dsl.selectCount().from(c).where(where(c) && isNotDeleted).fetchOne().value1()
  }

  def create(name: String, url: Option[String] = None, createdAt: DateTime = DateTime.now)(implicit session: DBSession): Company = {
    val c = Tables.COMPANY

    val id = dsl.insertInto(c, c.NAME, c.URL, c.CREATED_AT)
      .values(name, url.orNull, createdAt)
      .returning(c.ID)
      .fetchOne().getId

    Company(id = id, name = name, url = url, createdAt = createdAt)
  }

  def save(m: Company)(implicit session: DBSession): Company = {
    dsl.update(c)
      .set(c.NAME, m.name)
      .set(c.URL, m.url.orNull)
      .where(c.ID === m.id && isNotDeleted)
      .execute()
    m
  }

  def destroy(id: Long)(implicit session: DBSession): Unit = {
    dsl.update(c)
      .set(c.DELETED_AT, DateTime.now)
      .where(c.ID === id && isNotDeleted)
      .execute()
  }

}
