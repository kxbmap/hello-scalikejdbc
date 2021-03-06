package models

import db.{Tables, tables}
import jooqs.DBSession
import jooqs.syntax._
import org.joda.time.DateTime
import org.jooq.{Condition, Record, RecordMapper}
import scala.collection.JavaConverters._
import scala.compat.java8.OptionConverters._

case class Skill(
    id: Long,
    name: String,
    createdAt: DateTime,
    deletedAt: Option[DateTime] = None) {

  def save()(implicit session: DBSession): Skill = Skill.save(this)

  def destroy()(implicit session: DBSession): Unit = Skill.destroy(id)
}

object Skill {

  def apply(s: tables.Skill): RecordMapper[Record, Skill] =
    record => Skill(
      id = record.get(s.ID),
      name = record.get(s.NAME),
      createdAt = record.get(s.CREATED_AT),
      deletedAt = record.getOpt(s.DELETED_AT)
    )

  def opt(s: tables.Skill): RecordMapper[Record, Option[Skill]] =
    record => record.getOpt(s.ID).map(_ => record.map(Skill(s)))

  val s = Tables.SKILL.as("s")
  private val isNotDeleted = s.DELETED_AT.isNull

  def find(id: Long)(implicit session: DBSession): Option[Skill] = {
    dsl.selectFrom(s).where(s.ID === id && isNotDeleted).fetchOptional(Skill(s)).asScala
  }

  def findAll()(implicit session: DBSession): List[Skill] = {
    dsl.selectFrom(s)
      .where(isNotDeleted)
      .orderBy(s.ID)
      .fetch(Skill(s)).asScala.toList
  }

  def countAll()(implicit session: DBSession): Int = {
    dsl.selectCount().from(s).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: tables.Skill => Condition)(implicit session: DBSession): List[Skill] = {
    dsl.selectFrom(s)
      .where(where(s) && isNotDeleted)
      .orderBy(s.ID)
      .fetch(Skill(s)).asScala.toList
  }

  def countBy(where: tables.Skill => Condition)(implicit session: DBSession): Int = {
    dsl.selectCount().from(s).where(where(s) && isNotDeleted).fetchOne().value1()
  }

  def create(name: String, createdAt: DateTime = DateTime.now)(implicit session: DBSession): Skill = {
    val s = Tables.SKILL

    val id = dsl.insertInto(s, s.NAME, s.CREATED_AT)
      .values(name, createdAt)
      .returning(s.ID)
      .fetchOne().getId

    Skill(id = id, name = name, createdAt = createdAt)
  }

  def save(m: Skill)(implicit session: DBSession): Skill = {
    dsl.update(s)
      .set(s.NAME, m.name)
      .where(s.ID === m.id && isNotDeleted)
      .execute()
    m
  }

  def destroy(id: Long)(implicit session: DBSession): Unit = {
    dsl.update(s)
      .set(s.DELETED_AT, DateTime.now)
      .where(s.ID === id && isNotDeleted)
      .execute()
  }

}
