package me.scidev5.application.util

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

open class EnvReader {

    protected fun get(
        id: String
    ) = System.getenv(id)
    protected inline fun <T> get(
        id:String,
        parse:(String)->T
    ) = get(id)?.let {
        try {
            parse(it)
        } catch (e: Throwable) {
            throw Error("Environment variable '$id' could not be parsed", e)
        }
    }
    protected inline fun <reified T> get(
        id:String,
        json: Json,
    ) = get(id)?.let {
        try {
            json.decodeFromString<T>(it)
        } catch (e: Throwable) {
            throw Error("Environment variable '$id' could not be parsed", e)
        }
    }

    protected inline fun <T> getRequired(
        id:String,
        parse:(String)->T
    ) = get(id,parse) ?: throw Error("Environment variable '$id' not supplied")
    protected fun getRequired(
        id:String
    ) = get(id) ?: throw Error("Environment variable '$id' not supplied")
    protected inline fun <reified T> getRequired(
        id:String,
        json: Json
    ) = get<T>(id,json) ?: throw Error("Environment variable '$id' not supplied")
}