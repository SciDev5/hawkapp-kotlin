package util

import kotlinx.browser.window
import kotlinx.js.Object
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response
import util.coroutine.KPromise

object Fetch {
    enum class Method {
        GET, POST;
    }

    inline fun <reified T, reified R> json(url: String, method: Method, body: T, crossinline handleResult: (R) -> Unit) {
        fetchRaw(
            url, method,
            Json.encodeToString(body)
        ) { res ->
            res.text().then { bodyStr ->
                handleResult(Json.decodeFromString(bodyStr))
            }
        }
    }
    inline fun <reified R> json(url: String, method: Method, crossinline handleResult: (R) -> Unit) {
        fetchRaw(
            url, method
        ) { res ->
            res.text().then { bodyStr ->
                handleResult(Json.decodeFromString(bodyStr))
            }
        }
    }
    suspend inline fun <reified T, reified R> post(url: String, body: T) : R {
        val dataPromise = KPromise<R>()
        json<T,R>(url, Method.POST,body) { dataPromise.resolve(it) }
        return dataPromise.await()
    }
    suspend inline fun <reified R> post(url: String) : R {
        val dataPromise = KPromise<R>()
        json<R>(url, Method.POST) { dataPromise.resolve(it) }
        return dataPromise.await()
    }

    inline fun <reified R> get(url: String, crossinline handleResult: (R) -> Unit) {
        fetchRaw(
            url, Method.GET
        ) { res ->
            res.text().then { bodyStr ->
                handleResult(Json.decodeFromString(bodyStr))
            }
        }
    }
    suspend inline fun <reified R> get(url: String) : R {
        val dataPromise = KPromise<R>()
        get<R>(url) { dataPromise.resolve(it) }
        return dataPromise.await()
    }


    fun fetchRaw(
        url: String,
        method: Method,
        body: String? = null,
        handleResponse: (Response) -> Unit
    ) {
        window.fetch(url, Object.create<RequestInit>(null).also {
            it.method = method.name
            if (body != null) {
                it.headers = js("""{"Content-Type":"application/json"}""")
                it.body = body
            }
        }).then(handleResponse).catch {
            println(">>> ERROR IN FETCH")
            throw it
        }
    }
}