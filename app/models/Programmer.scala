package models

import db.Tables
import java.sql.{Connection, Timestamp}
import org.joda.time.DateTime
import org.jooq._
import org.jooq.impl.DSL
import scala.collection.JavaConverters._

case class Programmer(
    id: Long,
    name: String,
    companyId: Option[Long] = None,
    company: Option[Company] = None,
    skills: Seq[Skill] = Nil,
    createdAt: DateTime,
    deletedAt: Option[DateTime] = None) {

  def save()(implicit conn: Connection): Programmer = Programmer.save(this)

  def destroy()(implicit conn: Connection): Unit = Programmer.destroy(id)

  import Tables.{PROGRAMMER_SKILL => ps}

  def addSkill(skill: Skill)(implicit conn: Connection): Unit = {
    DSL.using(conn)
      .insertInto(ps, ps.PROGRAMMER_ID, ps.SKILL_ID)
      .values(id, skill.id)
      .execute()
  }

  def deleteSkill(skill: Skill)(implicit conn: Connection): Unit = {
    DSL.using(conn)
      .deleteFrom(ps)
      .where(ps.PROGRAMMER_ID.equal(id).and(ps.SKILL_ID.equal(skill.id)))
      .execute()
  }

}

object Programmer {

  def apply(p: db.tables.Programmer): RecordMapper[Record, Programmer] = new RecordMapper[Record, Programmer] {
    def map(record: Record): Programmer = Programmer(
      id = record.getValue(p.ID),
      name = record.getValue(p.NAME),
      companyId = Option(record.getValue(p.COMPANY_ID)).map(_.toLong),
      createdAt = new DateTime(record.getValue(p.CREATED_TIMESTAMP)),
      deletedAt = Option(record.getValue(p.DELETED_TIMESTAMP)).map(new DateTime(_))
    )
  }

  def apply(p: db.tables.Programmer, c: db.tables.Company): RecordMapper[Record, Programmer] = new RecordMapper[Record, Programmer] {
    val prm = Programmer(p)
    val crm = Company.opt(c)
    def map(record: Record): Programmer = prm.map(record).copy(company = crm.map(record))
  }

  val p = Tables.PROGRAMMER.as("p")

  private val (c, s, ps) = (Company.c, Skill.s, ProgrammerSkill.ps)

  // reusable part of SQL
  private val isNotDeleted = p.DELETED_TIMESTAMP.isNull

  // find by primary key
  def find(id: Long)(implicit conn: Connection): Option[Programmer] = {
    DSL.using(conn)
      .select(p.fields() ++ c.fields() ++ s.fields(): _*)
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID.equal(c.ID).and(c.DELETED_AT.isNull))
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID.equal(p.ID))
      .leftOuterJoin(s).on(ps.SKILL_ID.equal(s.ID).and(s.DELETED_AT.isNull))
      .where(p.ID.equal(id).and(isNotDeleted))
      .fetchGroups(p.ID).asScala
      .get(id)
      .map(rs => rs.get(0).map(Programmer(p, c)).copy(skills = rs.map(Skill.opt(s)).asScala.flatten))
  }

  // programmer with company(optional) with skills(many)
  def findAll()(implicit conn: Connection): List[Programmer] = {
    DSL.using(conn)
      .select(p.fields() ++ c.fields() ++ s.fields(): _*)
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID.equal(c.ID).and(c.DELETED_AT.isNull))
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID.equal(p.ID))
      .leftOuterJoin(s).on(ps.SKILL_ID.equal(s.ID).and(s.DELETED_AT.isNull))
      .where(isNotDeleted)
      .fetchGroups(p.ID).asScala
      .map {
        case (_, rs) => rs.get(0).map(Programmer(p, c)).copy(skills = rs.map(Skill.opt(s)).asScala.flatten)
      }(collection.breakOut)
  }

  def findNoSkillProgrammers()(implicit conn: Connection): List[Programmer] = {
    DSL.using(conn)
      .select(p.fields() ++ c.fields(): _*)
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID.equal(c.ID))
      .where(p.ID.notIn(DSL.selectDistinct(ps.PROGRAMMER_ID).from(ps)).and(isNotDeleted))
      .orderBy(p.ID)
      .fetch(Programmer(p, c)).asScala.toList
  }

  def countAll()(implicit conn: Connection): Int = {
    DSL.using(conn).selectCount().from(p).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: Condition, withCompany: Boolean = true)(implicit conn: Connection): List[Programmer] = {
    val mapper = if (withCompany) Programmer(p, c) else Programmer(p)
    val q = DSL.using(conn)
      .select(p.fields() ++ c.fields(): _*)
      .from(p)

    (if (withCompany) q.leftOuterJoin(c).on(p.COMPANY_ID.equal(c.ID).and(c.DELETED_AT.isNull)) else q)
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID.equal(p.ID))
      .leftOuterJoin(s).on(ps.SKILL_ID.equal(s.ID).and(s.DELETED_AT.isNull))
      .where(where.and(isNotDeleted))
      .fetchGroups(p.ID).asScala
      .map {
        case (_, rs) =>
          rs.get(0).map(mapper).copy(skills = rs.map(Skill.opt(s)).asScala.flatten)
      }(collection.breakOut)
  }

  def countBy(where: Condition)(implicit conn: Connection): Int = {
    DSL.using(conn).selectCount().from(p).where(where.and(isNotDeleted)).fetchOne().value1()
  }

  def create(name: String, companyId: Option[Long] = None, createdAt: DateTime = DateTime.now)(implicit conn: Connection): Programmer = {
    if (companyId.map(Company.find).exists(_.isEmpty)) {
      throw new IllegalArgumentException(s"Company is not found! (companyId: $companyId)")
    }
    val p = Tables.PROGRAMMER

    val id = DSL.using(conn)
      .insertInto(p, p.NAME, p.COMPANY_ID, p.CREATED_TIMESTAMP)
      .values(name, companyId.map(Long.box).orNull, new Timestamp(createdAt.getMillis))
      .returning(p.ID)
      .fetchOne().getId

    Programmer(
      id = id,
      name = name,
      companyId = companyId,
      company = companyId.flatMap(id => Company.find(id)),
      createdAt = createdAt)
  }

  def save(m: Programmer)(implicit conn: Connection): Programmer = {
    DSL.using(conn)
      .update(p)
      .set(p.NAME, m.name)
      .set(p.COMPANY_ID, m.companyId.map(Long.box).orNull)
      .where(p.ID.equal(m.id).and(isNotDeleted))
      .execute()
    m
  }

  def destroy(id: Long)(implicit conn: Connection): Unit = {
    DSL.using(conn)
      .update(p)
      .set(p.DELETED_TIMESTAMP, new Timestamp(DateTime.now.getMillis))
      .where(p.ID.equal(id).and(isNotDeleted))
      .execute()
  }

}
