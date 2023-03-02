import data.TimestampedId
import data.user.UserData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import util.*
import util.coroutine.UntilLock
import wsTransaction.KWSTransactor
import wsTransaction.syncReceive

@OptIn(DelicateCoroutinesApi::class)
class User private constructor(
    data: UserData,
    private val txr: KWSTransactor,
    private val updates: NonBlockingUpdates<UserData> = NonBlockingUpdates()
) : IdSingletonMap.HasId<TimestampedId>,
    UpdateReceiver<UserData> by updates {
    override val id = data.id

    var data = data
        private set
    val username get() = data.username

    private val syncUntil = UntilLock()

    private fun forget(): Boolean {
        if (updates.isWatched) return false
        syncUntil.unlock()
        return true
    }

    init {
        GlobalScope.launch {
            txr.run(UserData.TransactionNames.SYNC) {
                syncReceive<UserData>(syncUntil) {
                    updates.send(it)
                }
                updates.close()
            }
        }
    }

    object Instances : IdSingletonFetchMap<User, TimestampedId>() {
        private lateinit var txr: KWSTransactor
        fun withTxr(txr: KWSTransactor): Instances {
            this.txr = txr
            return this
        }

        override suspend fun readFetch(id: TimestampedId) =
            txr.run(UserData.TransactionNames.GET) {
                sendReceive<TimestampedId.SerialBox, UserData?>(id.serial())
            }?.let {
                User(it, txr)
            }

        override fun forgetIfPossible(v: User) = v.forget()

        suspend fun lookupUsername(username: String) =
            txr.run(UserData.TransactionNames.LOOKUP_USERNAME) {
                sendReceive<Pair<Boolean,String>,UserData?>(Pair(false, username))
            }?.let {
                get(it.id)
            }
        suspend fun lookupUsernameLoose(username: String) =
            txr.run(UserData.TransactionNames.LOOKUP_USERNAME) {
                sendReceive<Pair<Boolean,String>,List<UserData>>(Pair(true,username))
            }.mapNotNull {
                get(it.id)
            }
    }
}