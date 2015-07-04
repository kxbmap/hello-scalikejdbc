package models

import db.Tables

case class ProgrammerSkill(programmerId: Long, skillId: Long)

object ProgrammerSkill {
  val ps = Tables.PROGRAMMER_SKILL.as("ps")
}
