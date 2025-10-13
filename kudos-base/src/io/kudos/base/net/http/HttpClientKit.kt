package io.kudos.base.net.http

import io.kudos.base.data.json.JsonKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.*
import java.nio.charset.StandardCharsets
import kotlin.reflect.typeOf


/**
 * Http Client工具类。
 *
 * 封装Java11的HttpClient，其具有以下特点：支持异步，支持reactive streams，同时也支持了HTTP2以及WebSocket
 *
 * @author K
 * @since 1.0.0
 */
object HttpClientKit {

    inline fun <reified T : Any> get(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "GET", block)
    }

    inline fun <reified T : Any> asyncGet(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest(url, "GET", block)
    }

    inline fun <reified T : Any> post(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "POST", block)
    }

    inline fun <reified T : Any> asyncPost(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "POST", block)
    }

    inline fun <reified T : Any> put(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "PUT", block)
    }

    inline fun <reified T : Any> asyncPut(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "PUT", block)
    }

    inline fun <reified T : Any> delete(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "DELETE", block)
    }

    inline fun <reified T : Any> asyncDelete(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "DELETE", block)
    }

    inline fun <reified T : Any> options(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "OPTIONS", block)
    }

    inline fun <reified T : Any> asyncOptions(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "OPTIONS", block)
    }

    inline fun <reified T : Any> connect(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "CONNECT", block)
    }

    inline fun <reified T : Any> asyncConnect(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "CONNECT", block)
    }

    inline fun <reified T : Any> trace(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "TRACE", block)
    }

    inline fun <reified T : Any> asyncTrace(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "TRACE", block)
    }

    inline fun <reified T : Any> patch(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "PATCH", block)
    }

    inline fun <reified T : Any> asyncPatch(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "PATCH", block)
    }

    inline fun <reified T : Any> request(
        url: String,
        method: String,
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<T> {
        val clientBuilder = HttpClient.newBuilder()
        val requestBuilder = HttpRequest.newBuilder().apply {
            uri(URI.create(url))
            method(method, BodyPublishers.noBody())
            block()
        }
        val bodyHandler = createBodyHandler<T>()
        return request(clientBuilder, requestBuilder, bodyHandler)
    }

    inline fun <reified T : Any> asyncRequest(
        url: String,
        method: String,
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<T> {
        val clientBuilder = HttpClient.newBuilder()
        val requestBuilder = HttpRequest.newBuilder().apply {
            uri(URI.create(url))
            method(method, BodyPublishers.noBody())
            block()
        }
        val bodyHandler = createBodyHandler<T>()
        return asyncRequest(clientBuilder, requestBuilder, bodyHandler)
    }

    /**
     * 使用协程发起异步请求(不会阻塞线程)，并等待结果返回
     *
     * @param T 返回的response body类型
     * @param httpClientBuilder HttpClient.Builder对象，通过HttpClient.newBuilder()创建，并链式调用各配置方法
     * @param httpRequestBuilder HttpRequest.Builder对象，通过HttpRequest.newBuilder()创建，并链式调用各配置方法
     * @param bodyHandler HttpResponse.BodyHandler对象，通过HttpResponse.BodyHandlers.ofXXXX方法创建
     * @return 响应结果HttpResponse对象
     * @author K
     * @since 1.0.0
     */
    fun <T : Any> asyncRequest(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: HttpResponse.BodyHandler<T>
    ): HttpResponse<T> {
        return runBlocking {
            val result = async(Dispatchers.IO) {
                sendAsyncThenWait(httpClientBuilder, httpRequestBuilder, bodyHandler)
            }
            result.await()
        }
    }

    fun <T : Any> request(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: HttpResponse.BodyHandler<T>
    ): HttpResponse<T> {
        val client = createHttpClient(httpClientBuilder)
        val request = httpRequestBuilder.build()
        return client.send(request, bodyHandler)
    }

    private suspend fun <T : Any> sendAsyncThenWait(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: HttpResponse.BodyHandler<T>
    ): HttpResponse<T> {
        val request = httpRequestBuilder.build()
        val client = createHttpClient(httpClientBuilder)
        val response = client.sendAsync(request, bodyHandler)
        return response.await()
    }

    private fun createHttpRequest(httpRequestBuilder: HttpRequest.Builder): HttpRequest {
        return httpRequestBuilder.build()
    }

    private fun createHttpClient(httpClientBuilder: HttpClient.Builder): HttpClient {
        return httpClientBuilder.build()
    }

    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Any> createBodyHandler() : BodyHandler<T> {
        val kType = typeOf<T>()
        return when (kType) {
            typeOf<String>() -> BodyHandlers.ofString()
            typeOf<ByteArray>() -> BodyHandlers.ofByteArray()
            typeOf<InputStream>() -> BodyHandlers.ofInputStream()
            typeOf<Void>(), typeOf<Unit>() -> BodyHandlers.discarding() // 无响应体：只关心状态码/headers；Kotlin 世界常见用 Unit，这里也兼容 Java 的 Void
            typeOf<Array<Byte>>() -> BodyHandler {
                BodySubscribers.mapping(BodySubscribers.ofByteArray()) { bytes ->
                    bytes.toTypedArray()
                }
            }
            else -> {
                // 默认按 JSON 处理（支持复杂泛型）
                BodyHandler {
                    BodySubscribers.mapping(BodySubscribers.ofByteArray()) { bytes ->
                        JsonKit.fromJson<T>(bytes.toString(StandardCharsets.UTF_8))
                    }
                }
            }
        } as BodyHandler<T>
    }

    fun ofFormData(data: Map<Any, Any>): BodyPublisher {
//        .header("Content-Type","application/x-www-form-urlencoded")
        val builder = StringBuilder()
        data.forEach { (key, value) ->
            if (builder.isNotEmpty()) {
                builder.append("&")
            }
            builder.append(URLEncoder.encode(key.toString(), StandardCharsets.UTF_8))
            builder.append("=")
            builder.append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8))
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString())
    }

}