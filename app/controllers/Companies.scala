package controllers

import com.github.tototoshi.play2.json4s.native._
import javax.inject.Inject
import models._
import org.json4s._
import org.json4s.ext.JodaTimeSerializers
import play.api.data.Forms._
import play.api.data._
import play.api.data.validation.Constraints._
import play.api.db.Database
import play.api.mvc._

class Companies @Inject()(db: Database) extends Controller with Json4s {

  implicit val formats = DefaultFormats ++ JodaTimeSerializers.all

  def all = Action {
    db.withTransaction { implicit conn =>
      Ok(Extraction.decompose(Company.findAll()))
    }
  }

  def show(id: Long) = Action {
    db.withTransaction { implicit conn =>
      Company.find(id).map { company => Ok(Extraction.decompose(company)) } getOrElse NotFound
    }
  }

  case class CompanyForm(name: String, url: Option[String] = None)

  private val companyForm = Form(
    mapping(
      "name" -> text.verifying(nonEmpty),
      "url" -> optional(text)
    )(CompanyForm.apply)(CompanyForm.unapply)
  )

  def create = Action { implicit req =>
    companyForm.bindFromRequest.fold(
      formWithErrors => BadRequest("invalid parameters"),
      form => db.withTransaction { implicit conn =>
        val company = Company.create(name = form.name, url = form.url)
        Created.withHeaders(LOCATION -> s"/companies/${company.id}")
        NoContent
      }
    )
  }

  def delete(id: Long) = Action {
    db.withTransaction { implicit conn =>
      Company.find(id).map { company =>
        company.destroy()
        NoContent
      } getOrElse NotFound
    }
  }

}
