package models

import com.github.kxbmap.jooq.db.DBSession
import com.github.kxbmap.jooq.syntax._
import db.Tables
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

  def save()(implicit session: DBSession): Programmer = Programmer.save(this)

  def destroy()(implicit session: DBSession): Unit = Programmer.destroy(id)

  import Tables.{PROGRAMMER_SKILL => ps}

  def addSkill(skill: Skill)(implicit session: DBSession): Unit = {
    dsl.insertInto(ps, ps.PROGRAMMER_ID, ps.SKILL_ID)
      .values(id, skill.id)
      .execute()
  }

  def deleteSkill(skill: Skill)(implicit session: DBSession): Unit = {
    dsl.deleteFrom(ps)
      .where(ps.PROGRAMMER_ID === id and ps.SKILL_ID === skill.id)
      .execute()
  }

}

object Programmer {

  def apply(p: db.tables.Programmer): RecordMapper[Record, Programmer] = new RecordMapper[Record, Programmer] {
    def map(record: Record): Programmer = Programmer(
      id = record.get(p.ID),
      name = record.get(p.NAME),
      companyId = record.getOpt(p.COMPANY_ID),
      createdAt = record.get(p.CREATED_AT),
      deletedAt = record.getOpt(p.DELETED_AT)
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
  private val isNotDeleted = p.DELETED_AT.isNull

  // find by primary key
  def find(id: Long)(implicit session: DBSession): Option[Programmer] = {
    dsl.select(p.fields()).select(c.fields()).select(s.fields())
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID === c.ID and c.DELETED_AT.isNull)
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID === p.ID)
      .leftOuterJoin(s).on(ps.SKILL_ID === s.ID and s.DELETED_AT.isNull)
      .where(p.ID === id and isNotDeleted)
      .fetchGroups(p.ID).asScala
      .get(id)
      .map(rs => rs.get(0).map(Programmer(p, c)).copy(skills = rs.map(Skill.opt(s)).asScala.flatten))
  }

  // programmer with company(optional) with skills(many)
  def findAll()(implicit session: DBSession): List[Programmer] = {
    dsl.select(p.fields()).select(c.fields()).select(s.fields())
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID === c.ID and c.DELETED_AT.isNull)
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID === p.ID)
      .leftOuterJoin(s).on(ps.SKILL_ID === s.ID and s.DELETED_AT.isNull)
      .where(isNotDeleted)
      .fetchGroups(p.ID).asScala
      .map {
        case (_, rs) => rs.get(0).map(Programmer(p, c)).copy(skills = rs.map(Skill.opt(s)).asScala.flatten)
      }(collection.breakOut)
  }

  def findNoSkillProgrammers()(implicit session: DBSession): List[Programmer] = {
    dsl.select(p.fields()).select(c.fields())
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID === c.ID)
      .where(p.ID.notIn(DSL.selectDistinct(ps.PROGRAMMER_ID).from(ps)) and isNotDeleted)
      .orderBy(p.ID)
      .fetch(Programmer(p, c)).asScala.toList
  }

  def countAll()(implicit session: DBSession): Int = {
    dsl.selectCount().from(p).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: Condition, withCompany: Boolean = true)(implicit session: DBSession): List[Programmer] = {
    val prm = if (withCompany) Programmer(p, c) else Programmer(p)
    val srm = Skill.opt(s)
    dsl.select(p.fields())
      .mapIf(withCompany, _.select(c.fields()))
      .select(s.fields())
      .from(p)
      .mapIf(withCompany, _.leftOuterJoin(c).on(p.COMPANY_ID === c.ID and c.DELETED_AT.isNull))
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID === p.ID)
      .leftOuterJoin(s).on(ps.SKILL_ID === s.ID and s.DELETED_AT.isNull)
      .where(where and isNotDeleted)
      .fetchGroups(p.ID).asScala
      .map {
        case (_, rs) => rs.get(0).map(prm).copy(skills = rs.map(srm).asScala.flatten)
      }(collection.breakOut)
  }

  def countBy(where: Condition)(implicit session: DBSession): Int = {
    dsl.selectCount().from(p).where(where and isNotDeleted).fetchOne().value1()
  }

  def create(name: String, companyId: Option[Long] = None, createdAt: DateTime = DateTime.now)(implicit session: DBSession): Programmer = {
    if (companyId.map(Company.find).exists(_.isEmpty)) {
      throw new IllegalArgumentException(s"Company is not found! (companyId: $companyId)")
    }
    val p = Tables.PROGRAMMER

    val id = dsl.insertInto(p, p.NAME, p.COMPANY_ID, p.CREATED_AT)
      .values(name, companyId.boxed.orNull, createdAt)
      .returning(p.ID)
      .fetchOne().getId

    Programmer(
      id = id,
      name = name,
      companyId = companyId,
      company = companyId.flatMap(id => Company.find(id)),
      createdAt = createdAt)
  }

  def save(m: Programmer)(implicit session: DBSession): Programmer = {
    dsl.update(p)
      .set(p.NAME, m.name)
      .set(p.COMPANY_ID, m.companyId.boxed.orNull)
      .where(p.ID === m.id and isNotDeleted)
      .execute()
    m
  }

  def destroy(id: Long)(implicit session: DBSession): Unit = {
    dsl.update(p)
      .set(p.DELETED_AT, DateTime.now)
      .where(p.ID === id and isNotDeleted)
      .execute()
  }

}
