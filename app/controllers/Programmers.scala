package controllers

import com.github.tototoshi.play2.json4s.native._
import javax.inject.Inject
import jooqs.Database
import models._
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.mvc._

class Programmers @Inject()(db: Database) extends Controller with Json4s {

  implicit val formats = DefaultFormats ++ JodaTimeSerializers.all

  def all = Action {
    db.withTransaction { implicit session =>
      Ok(Extraction.decompose(Programmer.findAll))
    }
  }

  def show(id: Long) = Action {
    db.withTransaction { implicit session =>
      Programmer.find(id).map(programmer => Ok(Extraction.decompose(programmer))) getOrElse NotFound
    }
  }

  case class ProgrammerForm(name: String, companyId: Option[Long] = None)

  private val programmerForm = Form(
    mapping(
      "name" -> text.verifying(nonEmpty),
      "companyId" -> optional(longNumber)
    )(ProgrammerForm.apply)(ProgrammerForm.unapply)
  )

  def create = Action { implicit req =>
    programmerForm.bindFromRequest.fold(
      formWithErrors => BadRequest("invalid parameters"),
      form =>
        db.withTransaction { implicit session =>
          val programmer = Programmer.create(name = form.name, companyId = form.companyId)
          Created.withHeaders(LOCATION -> s"/programmers/${programmer.id}")
          NoContent
        }
    )
  }

  def addSkill(programmerId: Long, skillId: Long) = Action {
    db.withTransaction { implicit session =>
      Programmer.find(programmerId).map { programmer =>
        try {
          Skill.find(skillId).foreach(programmer.addSkill)
          Ok
        } catch {
          case e: Exception => Conflict
        }
      } getOrElse NotFound
    }
  }

  def deleteSkill(programmerId: Long, skillId: Long) = Action {
    db.withTransaction { implicit session =>
      Programmer.find(programmerId).map { programmer =>
        Skill.find(skillId).foreach(programmer.deleteSkill)
        Ok
      } getOrElse NotFound
    }
  }

  def joinCompany(programmerId: Long, companyId: Long) = Action {
    db.withTransaction { implicit session =>
      Company.find(companyId).map { company =>
        Programmer.find(programmerId).map { programmer =>
          programmer.copy(companyId = Some(company.id)).save()
          Ok
        } getOrElse BadRequest("Programmer not found!")
      } getOrElse BadRequest("Company not found!")
    }
  }

  def leaveCompany(programmerId: Long) = Action {
    db.withTransaction { implicit session =>
      Programmer.find(programmerId).map { programmer =>
        programmer.copy(companyId = None).save()
        Ok
      } getOrElse BadRequest("Programmer not found!")
    }
  }

  def delete(id: Long) = Action {
    db.withTransaction { implicit session =>
      Programmer.find(id).map { programmer =>
        programmer.destroy()
        NoContent
      } getOrElse NotFound
    }
  }

}
