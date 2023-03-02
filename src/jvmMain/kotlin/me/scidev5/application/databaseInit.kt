package me.scidev5.application

import me.scidev5.application.messageChannel.DMZone
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

fun databaseInit() {
    Database.connect("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", driver = "org.h2.Driver")

    transaction {
        SchemaUtils.create(User.DB.Users)
        SchemaUtils.create(DMZone.DB.DMZoneMembers)
        SchemaUtils.create(DMZone.DB.DMMembers)
        SchemaUtils.create(DMZone.DB.DMZones)
    }
}