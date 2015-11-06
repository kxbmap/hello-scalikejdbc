package models

import db.{Tables, tables}
import jooqs.DBSession
import jooqs.syntax._
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
      .where(ps.PROGRAMMER_ID === id && ps.SKILL_ID === skill.id)
      .execute()
  }

}

object Programmer {

  def apply(p: tables.Programmer): RecordMapper[Record, Programmer] =
    record => Programmer(
      id = record.get(p.ID),
      name = record.get(p.NAME),
      companyId = record.getOpt(p.COMPANY_ID),
      createdAt = record.get(p.CREATED_AT),
      deletedAt = record.getOpt(p.DELETED_AT)
    )

  def apply(p: tables.Programmer, c: tables.Company): RecordMapper[Record, Programmer] =
    record => record.map(Programmer(p)).copy(company = record.map(Company.opt(c)))

  val p = Tables.PROGRAMMER.as("p")

  private val (c, s, ps) = (Company.c, Skill.s, ProgrammerSkill.ps)

  // reusable part of SQL
  private val isNotDeleted = p.DELETED_AT.isNull

  // find by primary key
  def find(id: Long)(implicit session: DBSession): Option[Programmer] = {
    dsl.select(p.fields()).select(c.fields()).select(s.fields())
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID === c.ID && c.DELETED_AT.isNull)
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID === p.ID)
      .leftOuterJoin(s).on(ps.SKILL_ID === s.ID && s.DELETED_AT.isNull)
      .where(p.ID === id && isNotDeleted)
      .fetchGroups(Programmer(p, c), Skill.opt(s)).asScala
      .collectFirst {
        case (prg, skills) => prg.copy(skills = skills.asScala.flatten)
      }
  }

  // programmer with company(optional) with skills(many)
  def findAll()(implicit session: DBSession): List[Programmer] = {
    dsl.select(p.fields()).select(c.fields()).select(s.fields())
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID === c.ID && c.DELETED_AT.isNull)
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID === p.ID)
      .leftOuterJoin(s).on(ps.SKILL_ID === s.ID && s.DELETED_AT.isNull)
      .where(isNotDeleted)
      .fetchGroups(Programmer(p, c), Skill.opt(s)).asScala
      .map {
        case (prg, skills) => prg.copy(skills = skills.asScala.flatten)
      }(collection.breakOut)
  }

  def findNoSkillProgrammers()(implicit session: DBSession): List[Programmer] = {
    dsl.select(p.fields()).select(c.fields())
      .from(p)
      .leftOuterJoin(c).on(p.COMPANY_ID === c.ID)
      .where(p.ID.notIn(DSL.selectDistinct(ps.PROGRAMMER_ID).from(ps)) && isNotDeleted)
      .orderBy(p.ID)
      .fetch(Programmer(p, c)).asScala.toList
  }

  def countAll()(implicit session: DBSession): Int = {
    dsl.selectCount().from(p).where(isNotDeleted).fetchOne().value1()
  }

  def findAllBy(where: tables.Programmer => Condition, withCompany: Boolean = true)(implicit session: DBSession): List[Programmer] = {
    val prm = if (withCompany) Programmer(p, c) else Programmer(p)
    dsl.select(p.fields())
      .mapIf(withCompany, _.select(c.fields()))
      .select(s.fields())
      .from(p)
      .mapIf(withCompany, _.leftOuterJoin(c).on(p.COMPANY_ID === c.ID && c.DELETED_AT.isNull))
      .leftOuterJoin(ps).on(ps.PROGRAMMER_ID === p.ID)
      .leftOuterJoin(s).on(ps.SKILL_ID === s.ID && s.DELETED_AT.isNull)
      .where(where(p) && isNotDeleted)
      .fetchGroups(prm, Skill.opt(s)).asScala
      .map {
        case (prg, skills) => prg.copy(skills = skills.asScala.flatten)
      }(collection.breakOut)
  }

  def countBy(where: tables.Programmer => Condition)(implicit session: DBSession): Int = {
    dsl.selectCount().from(p).where(where(p) && isNotDeleted).fetchOne().value1()
  }

  def create(name: String, companyId: Option[Long] = None, createdAt: DateTime = DateTime.now)(implicit session: DBSession): Programmer = {
    if (companyId.map(Company.find).exists(_.isEmpty)) {
      throw new IllegalArgumentException(s"Company is not found! (companyId: $companyId)")
    }
    val p = Tables.PROGRAMMER

    val id = dsl.insertInto(p, p.NAME, p.COMPANY_ID, p.CREATED_AT)
      .values(name, companyId.box.orNull, createdAt)
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
      .set(p.COMPANY_ID, m.companyId.box.orNull)
      .where(p.ID === m.id && isNotDeleted)
      .execute()
    m
  }

  def destroy(id: Long)(implicit session: DBSession): Unit = {
    dsl.update(p)
      .set(p.DELETED_AT, DateTime.now)
      .where(p.ID === id && isNotDeleted)
      .execute()
  }

}
