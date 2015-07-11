package controllers

import com.github.kxbmap.jooq.db.Database
import com.github.tototoshi.play2.json4s.native._
import javax.inject.Inject
import models._
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.mvc._

class Skills @Inject()(db: Database) extends Controller with Json4s {

  implicit val formats = DefaultFormats ++ JodaTimeSerializers.all

  def all = Action {
    db.withTransaction { implicit session =>
      Ok(Extraction.decompose(Skill.findAll))
    }
  }

  def show(id: Long) = Action {
    db.withTransaction { implicit session =>
      Skill.find(id).map(skill => Ok(Extraction.decompose(skill))) getOrElse NotFound
    }
  }

  case class SkillForm(name: String)

  private val skillForm = Form(
    mapping("name" -> text.verifying(nonEmpty))(SkillForm.apply)(SkillForm.unapply)
  )

  def create = Action { implicit req =>
    skillForm.bindFromRequest.fold(
      formWithErrors => BadRequest("invalid parameters"),
      form =>
        db.withTransaction { implicit session =>
          val skill = Skill.create(name = form.name)
          Created.withHeaders(LOCATION -> s"/skills/${skill.id}")
          NoContent
        }
    )
  }

  def delete(id: Long) = Action {
    db.withTransaction { implicit session =>
      Skill.find(id).map { skill =>
        skill.destroy()
        NoContent
      } getOrElse NotFound
    }
  }

}
