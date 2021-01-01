package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import models._

import play.api.db.slick.DatabaseConfigProvider
import scala.concurrent.ExecutionContext
import play.api.db.slick.HasDatabaseConfigProvider
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Future

@Singleton
class StudentController @Inject() (
    protected val dbConfigProvider: DatabaseConfigProvider,
    val cc: ControllerComponents
)(implicit ec: ExecutionContext)
    extends AbstractController(cc)
    with HasDatabaseConfigProvider[JdbcProfile] {

  private val scModel: SchoolClassModel = new SchoolClassModel(db)
  private val model: StudentModel = new StudentModel(db)

  // wrapper that takes in a null function and returns a Future[mvc.Result]
                                                      // needs the request and other parameters implicitly
  def withAuthentication(f: () => Future[mvc.Result])(implicit request: Request[AnyContent], id: Int, classSecret: String) = { 
    val accepted: Future[Boolean] = scModel.validateAccess(id, classSecret)
      accepted.flatMap(a => {
        if (a) {
          request.headers.get("teacherSecret") match {
            case Some(teacherSecret) => {
              scModel.getTeacher(id, teacherSecret).flatMap(result =>
                result match {
                  case Some(teacher) =>
                    f() //return whatever the passed function returns
                  case None => Future.successful(Forbidden("Wrong teacherSecret")) //return
                }
              )
            }
            case None =>
              Future.successful(
                BadRequest("No teacherSecret provided")
              ) //return
          }
        } else {
          Future.successful(
            NotFound("Schoolclass with that id not found or wrong classSecret")
          ) //return
        }
      })
  }


    def getSignups(implicit id: Int, classSecret: String): play.api.mvc.Action[play.api.mvc.AnyContent] = Action.async {
    implicit request: Request[AnyContent] =>
   
    // this is the body of this method put into a null function which is the passed to the authentication wrapper method
    val body = {() => model.getNumberOfStudents(id).map(number => Ok(Json.toJson(number)))} //return

    // this calls the wrapper method
    withAuthentication(body)
  }
}
