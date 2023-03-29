package clientData

import data.TimestampedId
import data.channel.DMZoneData
import data.user.UserData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import util.*
import util.coroutine.UntilLock
import wsTransaction.KWSTransactor
import wsTransaction.syncReceive


@OptIn(DelicateCoroutinesApi::class)
class DMZone private constructor(
    data: DMZoneData,
    private val txr: KWSTransactor,
    private val updates: NonBlockingUpdates<DMZoneData> = NonBlockingUpdates()
) : IdSingletonMap.HasId<TimestampedId>,
    UpdateReceiver<DMZoneData> by updates {
    override val id = data.id

    var data = data
        private set


    private val syncUntil = UntilLock()

    private fun forget(): Boolean {
        if (updates.isWatched) return false
        syncUntil.unlock()
        return true
    }

    init {
        GlobalScope.launch {
            txr.run<Throwable?>(DMZoneData.TransactionNames.SYNC) {
                send(id.serial())
                if (!nextData<Boolean>())
                    return@run Error("DMZone not found for sync")
                syncReceive<DMZoneData>(syncUntil) {
                    updates.send(it)
                }
                updates.close()
                return@run null
            }
                ?.also { throw it }
        }
    }

    object Instances : IdSingletonFetchMap<DMZone, TimestampedId>(), WithTxr by WithTxr() {
        override suspend fun readFetch(id: TimestampedId) =
            txr.run(DMZoneData.TransactionNames.GET) {
                sendReceive<TimestampedId.SerialBox, DMZoneData?>(id.serial())
            }?.let {
                DMZone(it, txr)
            }

        override fun forgetIfPossible(v: DMZone) = v.forget()

        fun watchList(
            untilStopWatching: UntilLock
        ) = flow {
            txr.run(DMZoneData.TransactionNames.WATCH_LIST) {
                syncReceive<Set<DMZoneData>>(untilStopWatching) {
                    emit(it)
                }
            }
        }

        suspend fun create(
            writers: Set<User>,
            readonly: Set<User> = emptySet(),
        ) =
            txr.run(DMZoneData.TransactionNames.CREATE) {
                sendReceive<Pair<List<UserData>,List<UserData>>, TimestampedId.SerialBox>(
                    Pair(
                        writers.map { it.data },
                        readonly.map { it.data }
                    ),
                ).v
            }

        suspend fun leave(
            zone: DMZoneData
        ) =
            txr.run(DMZoneData.TransactionNames.LEAVE) {
                send(zone.id.serial())
            }
    }
}