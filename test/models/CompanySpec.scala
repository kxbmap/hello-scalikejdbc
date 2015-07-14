package models

import com.github.kxbmap.jooqs.syntax._
import db.Tables
import org.specs2.mutable._
import utils.AutoRollback

class CompanySpec extends Specification {

  trait AutoRollbackWithFixture extends AutoRollback {
    dsl.deleteFrom(Tables.COMPANY).execute()

    Company.create("Typesafe", Some("http://typesafe.com"))
    Company.create("Oracle")
    Company.create("Amazon")
  }

  "Company" should {
    "find by primary keys" in new AutoRollbackWithFixture {
      val id = Company.findAll().head.id
      val maybeFound = Company.find(id)
      maybeFound.isDefined should beTrue
    }
    "find all records" in new AutoRollbackWithFixture {
      val allResults = Company.findAll()
      allResults.size should_== 3
    }
    "count all records" in new AutoRollbackWithFixture {
      val count = Company.countAll()
      count should_== 3L
    }
    "find by where clauses" in new AutoRollbackWithFixture {
      val results = Company.findAllBy(_.URL.isNull)
      results.size should_== 2
    }
    "count by where clauses" in new AutoRollbackWithFixture {
      val count = Company.countBy(_.URL.isNotNull)
      count should_== 1L
    }
    "create new record" in new AutoRollbackWithFixture {
      val newCompany = Company.create("Microsoft")
      newCompany.id should not beNull
    }
    "save a record" in new AutoRollbackWithFixture {
      val entity = Company.findAll().head
      entity.copy(name = "Google").save()
      val updated = Company.find(entity.id).get
      updated.name should_== "Google"
    }
    "destroy a record" in new AutoRollbackWithFixture {
      val entity = Company.findAll().head
      entity.destroy()
      val shouldBeNone = Company.find(entity.id)
      shouldBeNone.isDefined should beFalse
      Company.countAll should_== 2L
    }
  }

}

