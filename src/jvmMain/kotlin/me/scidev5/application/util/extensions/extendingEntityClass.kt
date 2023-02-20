package me.scidev5.application.util.extensions

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SqlExpressionBuilder

fun <ID : Comparable<ID>, T : Entity<ID>> EntityClass<ID, T>.findFirst(
    op: SqlExpressionBuilder.() -> Op<Boolean>
) = find(op).let {
    if (it.empty())
        null
    else
        it.first()
}