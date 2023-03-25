package me.scidev5.application

import data.TimestampedId
import data.push.PushSubscriptionInfoKT
import data.user.UserData
import me.scidev5.application.push.convert
import me.scidev5.application.push.send
import me.scidev5.application.util.extensions.TimestampedIdEntity
import me.scidev5.application.util.extensions.TimestampedIdEntityClass
import me.scidev5.application.util.extensions.TimestampedIdTable
import me.scidev5.application.util.extensions.findFirst
import nl.martijndwars.webpush.Notification
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt
import util.IdSingletonMap
import util.NonBlockingUpdates
import util.UpdateReceiver
import wsTransaction.KWSTransactionHandle
import wsTransaction.syncSend

class User private constructor(
    val ref: DB.User,
    private val updates: NonBlockingUpdates<UserData> = NonBlockingUpdates()
) : UpdateReceiver<UserData> by updates,
    IdSingletonMap.HasId<TimestampedId> {

    override val id = ref.id.value
    val data
        get() = transaction {
            UserData(
                id = this@User.id,
                username = username
            )
        }

    val subscriptions = mutableSetOf<PushSubscriptionInfoKT>()
    fun sendNotification(content: String) {
        for (sub in subscriptions)
            Notification(sub.convert(), content).send()
    }

    var username
        get() = ref.username
        set(value) {
            transaction {
                ref.username = value
            }
            updates.send(data)
        }

    private var nRefs = 0
    fun abandon() {
        nRefs--
        Instances.forget(this)
    }
    fun <T> use(block:()->T) {
        nRefs++
        block()
        abandon()
    }

    fun forget(): Boolean {
        if (nRefs > 0) return false

        return true
    }

    fun checkPassword(password: String) =
        BCrypt.checkpw(password, transaction {
            ref.passHash
        })

    object Instances : IdSingletonMap<User, TimestampedId, UserData.Creation>() {
        override fun readFetch(id: TimestampedId) =
            transaction {
                DB.User.findById(id)
            }?.let { User(it) }

        override fun alsoOnGet(v: User) {
            v.nRefs++
        }
        override fun forgetIfPossible(v: User) = v.forget()
        override fun createFetch(data: UserData.Creation) =
            User(transaction {
                DB.User.new {
                    username = data.username
                    passHash = BCrypt.hashpw(data.password, BCrypt.gensalt())
                }
            })

        fun byUsername(username: String) = transaction {
            DB.User.findFirst {
                DB.Users.username eq username
            }
        }?.let {
            Instances[it.id.value]
        }

        fun likeUsername(username: String) = transaction {
            DB.User.find {
                DB.Users.username like "%$username%"
            }
        }.mapNotNull {
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
        }
    }

    object Handle {
        fun sync() = KWSTransactionHandle(UserData.TransactionNames.SYNC) {
            val user = Instances[nextData<TimestampedId.SerialBox>().v]
                ?: run {
                    send(false)
                    return@KWSTransactionHandle
                }
            send(true)

            val (watch, endWatch) = user.watch()
            syncSend(watch, cleanup = { endWatch() })

        }
        fun get() = KWSTransactionHandle(UserData.TransactionNames.GET) {
            send(Instances[nextData<TimestampedId.SerialBox>().v]?.data)
        }
        fun lookupUsername() = KWSTransactionHandle(UserData.TransactionNames.LOOKUP_USERNAME) {
            val (looseSearch, username) = nextData<Pair<Boolean,String>>()

            println("LOOSE: $looseSearch, UNM: $username")
            if (looseSearch) { // loose search mode
                send(Instances.likeUsername(username).map { it.data })
            } else {
                send(Instances.byUsername(username)?.data)
            }
        }
    }
}