package me.scidev5.application

import data.TimestampedId
import me.scidev5.application.messageChannel.DMZone
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

fun databaseInit() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    transaction {
        SchemaUtils.create(User.DB.Users)
        SchemaUtils.create(DMZone.DB.DMZoneMembers)
        SchemaUtils.create(DMZone.DB.DMMembers)
        SchemaUtils.create(DMZone.DB.DMZones)
    }

    // TEST DATA
    transaction {
        User.DB.User.new(TimestampedId(3735928559)) {
            username = "a"
            passHash = BCrypt.hashpw("", BCrypt.gensalt())
        }
        User.DB.User.new(TimestampedId(389384133)) {
            username = "b"
            passHash = BCrypt.hashpw("", BCrypt.gensalt())
        }
    }
}