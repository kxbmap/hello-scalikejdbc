package models

import db.Tables
import jooqs.syntax._
import org.specs2.mutable._
import utils.AutoRollback

class ProgrammerSpec extends Specification {

  trait AutoRollbackWithFixture extends AutoRollback {
    dsl.deleteFrom(Tables.PROGRAMMER_SKILL).execute()
    dsl.deleteFrom(Tables.PROGRAMMER).execute()
    dsl.deleteFrom(Tables.SKILL).execute()
    dsl.deleteFrom(Tables.COMPANY).execute()

    val scala = Skill.create("Scala")
    val java = Skill.create("Java")
    val ruby = Skill.create("Ruby")

    val programmer = Programmer.create("seratch")
    programmer.addSkill(scala)
    programmer.addSkill(java)
    programmer.addSkill(ruby)

    val company = Company.create("M3")
    programmer.copy(companyId = Some(company.id)).save()

    Programmer.create("no_skill_programmer")

    val anon = Programmer.create("anonymous")
    anon.addSkill(java)

    Programmer.create("should_be_deteled").destroy()
  }

  "Programmer" should {
    "find with skills" in new AutoRollbackWithFixture {
      val seratch = Programmer.findAllBy(_.NAME.equal("seratch")).head
      seratch.skills.map(_.name) should_== Seq("Scala", "Java", "Ruby")
    }
    "find no skill programmers" in new AutoRollbackWithFixture {
      val noSkillProgrammers = Programmer.findNoSkillProgrammers()
      noSkillProgrammers.size should_== 1
    }
    "find by primary keys" in new AutoRollbackWithFixture {
      val id = Programmer.findAll().head.id
      val maybeFound = Programmer.find(id)
      maybeFound.isDefined should beTrue
    }
    "find all records" in new AutoRollbackWithFixture {
      val allResults = Programmer.findAll()
      allResults.size should_== 3
    }
    "count all records" in new AutoRollbackWithFixture {
      val count = Programmer.countAll()
      count should_== 3L
    }
    "find by where clauses" in new AutoRollbackWithFixture {
      val results = Programmer.findAllBy(_.COMPANY_ID.isNotNull)
      results.head.name should_== "seratch"
    }
    "find by where clauses without company" in new AutoRollbackWithFixture {
      val results = Programmer.findAllBy(_.NAME.equal("seratch"), withCompany = false)
      results.head.company should beNone
    }
    "count by where clauses" in new AutoRollbackWithFixture {
      val count = Programmer.countBy(_.COMPANY_ID.isNull)
      count should_== 2L
    }
    "create new record" in new AutoRollbackWithFixture {
      val martin = Programmer.create("Martin")
      martin.id should not beNull
    }
    "save a record" in new AutoRollbackWithFixture {
      val entity = Programmer.findAll().head
      entity.copy(name = "Bob").save()
      val updated = Programmer.find(entity.id).get
      updated.name should_== "Bob"
    }
    "destroy a record" in new AutoRollbackWithFixture {
      val entity = Programmer.findAll().head
      entity.destroy()
      val shouldBeNone = Programmer.find(entity.id)
      shouldBeNone.isDefined should beFalse
      Programmer.countAll should_== 2L
    }
  }

}

