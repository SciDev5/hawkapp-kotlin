package me.scidev5.application

import data.TimestampedId
import data.user.UserData
import me.scidev5.application.util.extensions.TimestampedIdEntity
import me.scidev5.application.util.extensions.TimestampedIdEntityClass
import me.scidev5.application.util.extensions.TimestampedIdTable
import me.scidev5.application.util.extensions.findFirst
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import util.IdSingletonMap
import util.NonBlockingUpdates
import util.UpdateReceiver

class User private constructor(
    data: UserData,
    private val updates: NonBlockingUpdates<UserData> = NonBlockingUpdates()
) : UpdateReceiver<UserData> by updates,
    IdSingletonMap.HasId<TimestampedId> {

    override val id = data.id
    var data = data
        set(newValue) {
            if (newValue.id != field.id)
                throw Error("Cannot update user with another user's data")

            transaction {
                findThisInDB().username = newValue.username
            }
            field = newValue
            updates.send(data)
        }

    private fun findThisInDB() = DB.User.findById(this@User.id)
        ?: throw Error("User not found in database")

    fun checkPassword(password: String) =
        BCrypt.checkpw(password, transaction {
            findThisInDB().passHash
        })

    object Instances : IdSingletonMap<User, TimestampedId, UserData.Creation>() {
        override fun readFetch(id: TimestampedId) =
            transaction {
                DB.User.findById(id)
            }?.let { User(it.toData()) }

        override fun createFetch(data: UserData.Creation) =
            transaction {
                DB.User.new {
                    username = data.username
                    passHash = BCrypt.hashpw(data.password, BCrypt.gensalt())
                }
            }.let { User(it.toData()) }

        fun byUsername(username: String) = transaction {
            DB.User.findFirst {
                DB.Users.username eq username
            }
        }?.let {
            Instances[it.id.value]
        }
    }

    object DB {
        object Users : TimestampedIdTable() {
            val username = varchar("username", 64).uniqueIndex()
            val passHash = varchar("passHash", 60)
        }

        class User(id: EntityID<TimestampedId>) : TimestampedIdEntity(id) {
            companion object : TimestampedIdEntityClass<User>(Users)

            var username by Users.username
            var passHash by Users.passHash

            fun toData() = UserData(id = id.value, username = username)
        }
    }
}