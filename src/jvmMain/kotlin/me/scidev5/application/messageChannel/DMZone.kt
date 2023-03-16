package me.scidev5.application.messageChannel

import data.TimestampedId
import data.channel.ChannelMemberData
import data.channel.DMZoneData
import data.user.UserData
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import me.scidev5.application.User
import me.scidev5.application.util.extensions.TimestampedIdEntity
import me.scidev5.application.util.extensions.TimestampedIdEntityClass
import me.scidev5.application.util.extensions.TimestampedIdTable
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SizedCollection
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import util.IdSingletonMap
import util.MutableSetMap
import util.NonBlockingUpdates
import util.UpdateReceiver
import wsTransaction.KWSTransactionHandle
import wsTransaction.syncSend

class DMZone
private constructor(
    private val ref: DB.DMZone,
    private val updates: NonBlockingUpdates<DMZoneData> = NonBlockingUpdates()
) : UpdateReceiver<DMZoneData> by updates,
    IdSingletonMap.HasId<TimestampedId> {
    private var deleted = false

    override val id = ref.id.value

    private val membersMutable = mutableSetOf<Member>()
    val members: Set<Member> by this::membersMutable
    private fun updateMembers() {
        ref.members = SizedCollection(members.map { it.ref })
    }

    fun addMembers(vararg members: ChannelMemberData) = addMembers(members, true)
    private fun addMembers(members: Array<out ChannelMemberData>, update: Boolean) {
        if (deleted) return
        transaction {
            for (memberData in members) {
                val userRef = User.Instances[memberData.id]?.ref
                    ?: continue // ignore invalid
                val member = DB.DMMember.new {
                    user = userRef
                    writeAccess = memberData.writeAccess
                }
                membersMutable.add(Member(member))
            }
            updateMembers()
        }
        if (update)
            updates.send(data)
    }

    private fun delete() {
        if (deleted) return
        deleted = true
        for (member in members.map { it })
            member.delete(false)
        transaction {
            ref.delete()

            channelCurrent?.close()
        }
        updates.close()
    }

    private fun forget(): Boolean {
        if (deleted) return true
        if (updates.isWatched) return false

        deleted = true
        channelCurrent?.close()
        updates.close()
        return true
    }

    private var channelCurrent: MessageChannel? = null

    val channel
        get() = if (deleted) null else
            (channelCurrent ?: MessageChannel(PermissionSet()))
                .also {
                    if (it != channelCurrent)
                        it.onClose = this::onChannelClose
                    channelCurrent = it
                }

    private fun onChannelClose() {
        channelCurrent = null
    }

    val data
        get() = DMZoneData(
            id = id,
            members = members.map { it.data }.toTypedArray()
        )

    init {
        transaction {
            for (member in ref.members)
                membersMutable.add(Member(member))
        }
    }

    inner class PermissionSet : ChannelPermissionSet {
        override fun get(user: User): ChannelPermissions {
            val member = members.find { it.userId == user.id }
                ?: return ChannelPermissions(use = false)

            return ChannelPermissions(
                write = member.writeAccess,
            )
        }
    }

    inner class Member(
        val ref: DB.DMMember,
        private val updates: NonBlockingUpdates<ChannelMemberData> = NonBlockingUpdates()
    ) : UpdateReceiver<ChannelMemberData> by updates {
        private var deleted = false

        val userId = ref.user.id.value
        var writeAccess
            get() = ref.writeAccess
            set(value) {
                if (deleted) return
                transaction {
                    writeAccess = value
                }
                updates.send(data)
            }

        val data
            get() = transaction {
                ChannelMemberData(
                    id = ref.user.id.value,
                    writeAccess = writeAccess
                )
            }

        val isControllingMember get() = writeAccess
        fun delete(updateMembers: Boolean = true) {
            if (deleted) return
            deleted = true
            transaction {
                membersMutable.remove(this@Member)
                if (updateMembers)
                    updateMembers()
                ref.delete()
            }
            if (updateMembers)
                this@DMZone.updates.send(this@DMZone.data)
        }
    }

    object Instances : IdSingletonMap<DMZone, TimestampedId, DMZoneData>() {
        override fun readFetch(id: TimestampedId) =
            transaction {
                DB.DMZone.findById(id)
            }?.let { DMZone(it) }

        override fun forgetIfPossible(v: DMZone) = v.forget()
        override fun createFetch(data: DMZoneData) =
            DMZone(transaction {
                DB.DMZone.new {
                    members = SizedCollection(emptyList())
                }
            }).also {
                it.addMembers(data.members, false)
            }
    }

    object DB {
        object DMMembers : TimestampedIdTable() {
            val user = reference("user", User.DB.Users)
            val writeAccess = bool("writeAccess")
        }

        class DMMember(id: EntityID<TimestampedId>) : TimestampedIdEntity(id) {
            companion object : TimestampedIdEntityClass<DMMember>(DMMembers)

            var user by User.DB.User referencedOn DMMembers.user // many-to-one
            var writeAccess by DMMembers.writeAccess

            val zones by DMZone via DMZoneMembers
        }

        object DMZoneMembers : Table() {
            val user = reference("user", DMMembers)
            val zone = reference("zone", DMZones)
            override val primaryKey = PrimaryKey(user, zone)
        }

        object DMZones : TimestampedIdTable() {

        }

        class DMZone(id: EntityID<TimestampedId>) : TimestampedIdEntity(id) {
            companion object : TimestampedIdEntityClass<DMZone>(DMZones)

            var members by DMMember via DMZoneMembers
        }
    }

    object Handle {
        private val createDMForUserWatches = MutableSetMap<TimestampedId, SendChannel<DMZone>>()
        fun watchOwnList(
            user: User
        ) = KWSTransactionHandle(DMZoneData.TransactionNames.WATCH_LIST) {
            val zoneSource = Channel<DMZone>(Channel.UNLIMITED)
            val update = Channel<Set<DMZoneData>>()
            val zoneList = mutableSetOf<DMZone>()
            suspend fun update() = update.send(zoneList.map { it.data }.toSet())

            val alreadyInZones = mutableSetOf<TimestampedId>().also {
                transaction {
                    for (membership in DB.DMMember.find { DB.DMMembers.user eq user.id }) {
                        for (z in membership.zones) {
                            it.add(z.id.value)
                        }
                    }
                }
            }.mapNotNull {
                Instances[it]
            }
            zoneList.addAll(alreadyInZones)


            createDMForUserWatches.add(user.id, zoneSource)
            val listenJob = launch {
                fun watchZone(zone: DMZone, initialUpdate: Boolean) = launch {
                    zoneList.add(zone)
                    if (initialUpdate) update()
                    val (watch, endWatch) = zone.watch()
                    try {
                        for (newData in watch) {
                            if (newData.members.none { it.id == user.id })
                                break // if we've left the dm
                            update()
                        }
                        zoneList.remove(zone)
                        update()
                    } finally {
                        endWatch()
                    }
                }
                for (zone in alreadyInZones) watchZone(zone, false)
                update()
                for (zone in zoneSource) watchZone(zone, true)
            }

            syncSend(update, cleanup = {
                createDMForUserWatches.remove(user.id, zoneSource)
                zoneSource.close()
                listenJob.cancel()
            })

        }

        fun create(
            user: User
        ) = KWSTransactionHandle(DMZoneData.TransactionNames.CREATE) {
            val users = (nextData<List<UserData>>().mapNotNull {
                User.Instances[it.id]
            } + listOf(user)).toSet()

            val zone = Instances.create(
                DMZoneData(
                    TimestampedId(0),
                    users.map { ChannelMemberData(it.id, true) }.toTypedArray()
                )
            )
            for (u in users)
                createDMForUserWatches[u.id]
                    .forEach { it.send(zone) }
            send(zone.id.serial())
        }

        fun delete(
            user: User,
        ) = KWSTransactionHandle(DMZoneData.TransactionNames.LEAVE) {
            val zone = Instances[nextData<TimestampedId.SerialBox>().v]
                ?: return@KWSTransactionHandle
            for (membership in zone.members.filter { it.userId == user.id })
                membership.delete()
            if (zone.members.none { it.isControllingMember })
                zone.delete()
        }



        fun sync(
            user: User
        ) = KWSTransactionHandle(DMZoneData.TransactionNames.SYNC) {
            val zone = Instances[nextData<TimestampedId.SerialBox>().v]
                ?: run {
                    send(false)
                    return@KWSTransactionHandle
                }
            if (zone.members.none { it.userId == user.id }) {
                // Not allowed to know about DMs it's not from
                send(false)
                return@KWSTransactionHandle
            }
            send(true)

            val (watch, endWatch) = zone.watch()
            syncSend(watch, cleanup = { endWatch() })

        }
        fun get(
            user: User
        ) = KWSTransactionHandle(DMZoneData.TransactionNames.GET) {
            send(Instances[nextData<TimestampedId.SerialBox>().v]?.let { zone ->
                if (zone.members.none { it.userId == user.id })
                    // Not allowed to know about DMs it's not from
                    null
                else
                    zone.data
            })
        }
    }
}
