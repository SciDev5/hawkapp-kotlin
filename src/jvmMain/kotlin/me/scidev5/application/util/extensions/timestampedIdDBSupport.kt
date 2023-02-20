package me.scidev5.application.util.extensions

import data.TimestampedId
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.ColumnType
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.statements.api.PreparedStatementApi
import org.jetbrains.exposed.sql.vendors.currentDialect
import kotlin.random.Random

class TimestampedIdColumnType : ColumnType() {
    override fun sqlType(): String = currentDialect.dataTypeProvider.longType()
    override fun valueFromDB(value: Any): TimestampedId = TimestampedId(when (value) {
        is Long -> value
        is Number -> value.toLong()
        is String -> value.toLong()
        else -> error("Unexpected value of type Long: $value of ${value::class.qualifiedName}")
    })


    override fun setParameter(stmt: PreparedStatementApi, index: Int, value: Any?) {
        val v = if (value is TimestampedId) value.v else value
        super.setParameter(stmt, index, v)
    }

    override fun notNullValueToDB(value: Any): Any {
        val v = if (value is TimestampedId) value.v else value
        return super.notNullValueToDB(v)
    }
}
open class TimestampedIdTable(name: String = "", columnName: String = "id") : IdTable<TimestampedId>(name) {
    final override val id: Column<EntityID<TimestampedId>> = timestampedId(columnName).clientDefault {
        TimestampedId(Random.nextInt(),(System.currentTimeMillis()/1000).toInt())
    }.entityId()
    final override val primaryKey = PrimaryKey(id)
}
open class TimestampedIdEntity(id: EntityID<TimestampedId>) : Entity<TimestampedId>(id)

abstract class TimestampedIdEntityClass<out E : TimestampedIdEntity> constructor(
    table: IdTable<TimestampedId>,
    entityType: Class<E>? = null,
    entityCtor: ((EntityID<TimestampedId>) -> E)? = null
) : EntityClass<TimestampedId, E>(table, entityType, entityCtor)


fun Table.timestampedId(name: String): Column<TimestampedId> = registerColumn(name, TimestampedIdColumnType())