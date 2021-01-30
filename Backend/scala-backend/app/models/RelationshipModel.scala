package models

import slick.jdbc.PostgresProfile.api._
import scala.concurrent.ExecutionContext // the execution context is needed for concurrent execution
import models.Tables._
import scala.concurrent.Future

class RelationshipModel(db: Database)(implicit ec: ExecutionContext) {
  def createRelationship(relationshipCC: RelationshipCC): Future[Boolean] = {
    if (relationshipCC.sourceId == relationshipCC.targetId) {
      Future.successful(false)
    } else {
      db.run(
        Relationship += RelationshipRow(
          -1, // id is automatically set
          Some(relationshipCC.classId),
          Some(relationshipCC.sourceId),
          Some(relationshipCC.targetId)
        )
      ).map(rowsAffected => rowsAffected == 1)
    }
  }

  def removeRelationship(relationshipId: Int): Future[Boolean] = ???

  def getRelationship(relationshipId: Int): Future[RelationshipCC] = ???

  def getAllRelationIdsOfClass(classId: Int): Future[Seq[(Int, Int)]] = {
    db.run(Relationship.filter(x => (x.classid === classId)).result)
      .map(rows => rows.map(row => (row.sourceid.get, row.targetid.get)))
  }

  def rewireRelations(oldId: Int, newId: Int): Future[Boolean] = {
    val affectedRows: Future[Int] = db.run(Relationship.filter(_.targetid === oldId).map(row => (row.targetid)).update(Some(newId)))
    affectedRows.map(rows => rows > 0)
  }
}
