package models

import db.Tables
import java.sql.Connection
import org.joda.time.DateTime
import org.jooq.impl.DSL
import org.jooq.{Condition, Record, RecordMapper}
import scala.collection.JavaConverters._

case class Skill(
    id: Long,
    name: String,
    createdAt: DateTime,
    deletedAt: Option[DateTime] = None) {

  def save()(implicit conn: Connection): Skill = Skill.save(this)

  def destroy()(implicit conn: Connection): Unit = Skill.destroy(id)
}

object Skill {

  def apply(s: db.tables.Skill): RecordMapper[Record, Skill] = new RecordMapper[Record, Skill] {
    def map(record: Record): Skill = Skill(
      id = record.getValue(s.ID),
      name = record.getValue(s.NAME),
      createdAt = record.getValue(s.CREATED_AT),
      deletedAt = Option(record.getValue(s.DELETED_AT))
    )
  }

  def opt(s: db.tables.Skill): RecordMapper[Record, Option[Skill]] = new RecordMapper[Record, Option[Skill]] {
    val srm = Skill(s)
    def map(record: Record): Option[Skill] = Option(record.getValue(s.ID)).map(_ => srm.map(record))
  }

  val s = Tables.SKILL.as("s")
  private val isNotDeleted = s.DELETED_AT.isNull

  def find(id: Long)(implicit conn: Connection): Option[Skill] = {
    Option(DSL.using(conn).selectFrom(s).where(s.ID.equal(id).and(isNotDeleted)).fetchOne(Skill(s)))
  }

  def findAll()(implicit conn: Connection): List[Skill] = {
    DSL.using(conn)
      .selectFrom(s)
      .where(isNotDeleted)
      .orderBy(s.ID)
      .fetch(Skill(s)).asScala.toList
  }

  def countAll()(implicit conn: Connection): Int = {
    DSL.using(conn).selectCount().from(s).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: Condition)(implicit conn: Connection): List[Skill] = {
    DSL.using(conn)
      .selectFrom(s)
      .where(where.and(isNotDeleted))
      .orderBy(s.ID)
      .fetch(Skill(s)).asScala.toList
  }

  def countBy(where: Condition)(implicit conn: Connection): Int = {
    DSL.using(conn).selectCount().from(s).where(where.and(isNotDeleted)).fetchOne().value1()
  }

  def create(name: String, createdAt: DateTime = DateTime.now)(implicit conn: Connection): Skill = {
    val s = Tables.SKILL

    val id = DSL.using(conn)
      .insertInto(s, s.NAME, s.CREATED_AT)
      .values(name, createdAt)
      .returning(s.ID)
      .fetchOne().getId

    Skill(id = id, name = name, createdAt = createdAt)
  }

  def save(m: Skill)(implicit conn: Connection): Skill = {
    DSL.using(conn)
      .update(s)
      .set(s.NAME, m.name)
      .where(s.ID.equal(m.id).and(isNotDeleted))
      .execute()
    m
  }

  def destroy(id: Long)(implicit conn: Connection): Unit = {
    DSL.using(conn)
      .update(s)
      .set(s.DELETED_AT, DateTime.now)
      .where(s.ID.equal(id).and(isNotDeleted))
      .execute()
  }

}
