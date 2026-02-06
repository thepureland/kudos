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
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublisher
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.net.http.HttpResponse.*
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Duration
import java.util.*
import javax.net.ssl.SSLSession
import kotlin.reflect.typeOf


/**
 * Http Client工具类。
 *
 * 对 Java 11+ 的 `java.net.http.HttpClient` 做了轻量封装，提供：
 * - 同步/异步（协程等待）请求能力
 * - 基于 **reified 泛型** 的响应体自动反序列化（String/ByteArray/InputStream/Unit/自定义JSON等）
 * - 常见 HTTP 动词（GET/POST/PUT/DELETE/OPTIONS/CONNECT/TRACE/PATCH）的便捷方法
 *
 * 注意：
 * - 默认分支会将响应体按 **JSON** 解析为 `T`（使用 `JsonKit`），因此若响应并非 JSON，请在调用时显式指定 `T` 为具体类型（如 String/ByteArray）。
 * - `ByteBuffer` 的分支当前使用 `ofByteArrayConsumer {}`，此种写法会消费字节但**不返回** ByteBuffer 实例（更像丢弃/侧写语义），见方法内注释。
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
object HttpClientKit {

    // -------------- 便捷方法（同步/异步 + 常见HTTP动词）--------------

    /** 同步 GET；T 由调用处的泛型参数决定响应体类型 */
    inline fun <reified T : Any> get(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "GET", block)
    }

    /** 异步 GET；内部用协程等待 sendAsync 的结果并返回 */
    inline fun <reified T : Any> asyncGet(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest(url, "GET", block)
    }

    /** 同步 POST */
    inline fun <reified T : Any> post(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "POST", block)
    }

    /** 异步 POST */
    inline fun <reified T : Any> asyncPost(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "POST", block)
    }

    /** 同步 PUT */
    inline fun <reified T : Any> put(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "PUT", block)
    }

    /** 异步 PUT */
    inline fun <reified T : Any> asyncPut(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "PUT", block)
    }

    /** 同步 DELETE */
    inline fun <reified T : Any> delete(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "DELETE", block)
    }

    /** 异步 DELETE */
    inline fun <reified T : Any> asyncDelete(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "DELETE", block)
    }

    /** 同步 OPTIONS */
    inline fun <reified T : Any> options(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "OPTIONS", block)
    }

    /** 异步 OPTIONS */
    inline fun <reified T : Any> asyncOptions(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "OPTIONS", block)
    }

    /** 同步 CONNECT */
    inline fun <reified T : Any> connect(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "CONNECT", block)
    }

    /** 异步 CONNECT */
    inline fun <reified T : Any> asyncConnect(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "CONNECT", block)
    }

    /** 同步 TRACE */
    inline fun <reified T : Any> trace(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "TRACE", block)
    }

    /** 异步 TRACE */
    inline fun <reified T : Any> asyncTrace(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "TRACE", block)
    }

    /** 同步 PATCH */
    inline fun <reified T : Any> patch(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "PATCH", block)
    }

    /** 异步 PATCH */
    inline fun <reified T : Any> asyncPatch(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "PATCH", block)
    }

    // -------------- 统一的同步/异步入口（以 URL + method 为输入）--------------

    /**
     * 同步请求入口：根据 URL + HTTP 方法构建请求。
     * @param block 可在构建器上追加 header、timeout、body 等（默认已设置 noBody，可在 block 中覆盖）
     */
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
        val bodyHandler = __createBodyHandler<T>()      // 按 T 类型推导 BodyHandler
        return request(clientBuilder, requestBuilder, bodyHandler)
    }

    /**
     * 异步请求入口（协程等待）：与上面同步版本一致，只是最终通过 sendAsync + await。
     */
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
        val bodyHandler = __createBodyHandler<T>()
        return asyncRequest(clientBuilder, requestBuilder, bodyHandler)
    }

    // -------------- 底层执行（可复用传入的 Builder/Handler，以便更细粒度控制）--------------

    /**
     * 使用协程发起异步请求(不会阻塞线程)，并等待结果返回。
     *
     * 约定：
     * - 在 `Dispatchers.IO` 上启动子协程，避免阻塞主线程/测试线程。
     * - `sendAsync(...).await()` 利用 `kotlinx-coroutines-jdk8` 对 `CompletableFuture` 的扩展。
     */
    fun <T : Any> asyncRequest(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: BodyHandler<T>
    ): HttpResponse<T> {
        return runBlocking {
            val result = async(Dispatchers.IO) {
                sendAsyncThenWait(httpClientBuilder, httpRequestBuilder, bodyHandler)
            }
            result.await()
        }
    }

    /**
     * 同步执行：直接 `HttpClient.send(...)`。
     * - 适合对时序要求简单、或不需要并发的调用
     */
    fun <T : Any> request(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: BodyHandler<T>
    ): HttpResponse<T> {
        val client = createHttpClient(httpClientBuilder) // `.build()` 独立封装，便于后续注入/替换
        val request = httpRequestBuilder.build()
        return client.send(request, bodyHandler)
    }

    /**
     * 实际异步执行逻辑：
     * - build 请求
     * - client.sendAsync(request, handler)
     * - 使用 `await()` 等待
     */
    private suspend fun <T : Any> sendAsyncThenWait(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: BodyHandler<T>
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

    // -------------- 基于 reified T 的 BodyHandler 选择器 --------------

    /**
     * 根据调用处的 `T`（reified）动态选择 `BodyHandler<T>`。
     *
     * 已支持：
     * - String              -> `BodyHandlers.ofString()`
     * - ByteArray           -> `BodyHandlers.ofByteArray()`
     * - InputStream         -> `BodyHandlers.ofInputStream()`
     * - ByteBuffer          -> `BodyHandlers.ofByteArrayConsumer { }`（注意：该写法消费字节但不返回 ByteBuffer 实例，见下方注释）
     * - Void / Unit         -> `BodyHandlers.discarding()`（无响应体场景，典型如 204；只关心状态码/头）
     * - Array<Byte>         -> 将 `ofByteArray()` 的结果映射到 `Array<Byte>`
     * - 其他类型（默认）      -> 先取原始字节，再用 `JsonKit.fromJson<T>()` 解析为 JSON 对象/集合/泛型
     *
     * 风险/注意：
     * - `ByteBuffer` 分支当前写法：`ofByteArrayConsumer { }` 的返回类型为 `BodyHandler<Void>` 类别的消费器（适于“边消费边处理”），
     *   **不会**像 `mapping(ofByteArray()) { ByteBuffer.wrap(bytes) }` 那样返回 `ByteBuffer`。若调用方期望拿到 `ByteBuffer`，需要调整实现。
     * - 默认 JSON 分支：确保响应是合法 JSON，且 `JsonKit` 能正确解析为 `T`（包含复杂泛型）。
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T: Any> __createBodyHandler() : BodyHandler<T> {
        val kType = typeOf<T>()
        return when (kType) {
            typeOf<String>() -> BodyHandlers.ofString()                 // 文本响应
            typeOf<ByteArray>() -> BodyHandlers.ofByteArray()           // 原始字节
            typeOf<InputStream>() -> BodyHandlers.ofInputStream()       // 流（适合大文件/流式处理）
            typeOf<ByteBuffer>() -> BodyHandlers.ofByteArrayConsumer { } // 注意：消费器，不返回 ByteBuffer（见上方注释）
            typeOf<Void>(), typeOf<Unit>() -> BodyHandlers.discarding() // 无响应体：只关心状态码/headers；Kotlin 的 Unit 也做兼容
            typeOf<Array<Byte>>() -> BodyHandler {
                BodySubscribers.mapping(BodySubscribers.ofByteArray()) { bytes ->
                    bytes.toTypedArray()                                // 将原始 ByteArray 转为 Array<Byte>
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

    // -------------- x-www-form-urlencoded 的 BodyPublisher 构造 --------------

    /**
     * 将键值对 Map 按 `application/x-www-form-urlencoded` 规则编码为 `BodyPublisher`。
     *
     * 约定/细节：
     * - 使用 `URLEncoder.encode(..., UTF-8)` 编码 key 与 value。
     * - 键值对以 `&` 连接，key 与 value 之间以 `=` 连接。
     * - 该方法**仅返回**请求体，不会自动设置 `Content-Type` 头；调用方通常需要：
     *      `header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")`
     *
     * 示例结果：
     *   data = {"a b":"中 文", "x":"1+2=3"}
     *   -> "a+b=%E4%B8%AD+%E6%96%87&x=1%2B2%3D3"
     */
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
        return BodyPublishers.ofString(builder.toString())              // 生成字符串请求体
    }


    /**
     * 同步下载到指定文件（原子落盘：临时文件 -> 移动覆盖）
     *
     * @param url            下载链接
     * @param target         目标文件完整路径（不存在会创建父目录）
     * @param resume         是否尝试断点续传（若 target 已存在，则加 Range 头从现有大小续传；服务端需支持 206）
     * @param overwrite      当非续传且目标已存在时是否覆盖；若为 false 且已存在则直接返回 200 且不发请求
     * @param connectTimeout 连接超时（可选）
     * @param block          允许外部继续定制 HttpRequest（加 header、timeout、method 等；默认为 GET+noBody）
     * @return HttpResponse<Path>：body 为最终文件路径，statusCode 可用于判断 200/206/304 等
     */
    fun download(
        url: String,
        target: Path,
        resume: Boolean = false,
        overwrite: Boolean = true,
        connectTimeout: Duration? = Duration.ofSeconds(30),
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<Path> {
        // 准备目录与临时文件
        Files.createDirectories(target.parent)
        val tmp = target.resolveSibling(target.fileName.toString() + ".part")

        val clientBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
        if (connectTimeout != null) clientBuilder.connectTimeout(connectTimeout)

        val requestBuilder = HttpRequest.newBuilder().apply {
            uri(URI.create(url))
            method("GET", BodyPublishers.noBody())

            // 断点续传：若存在旧文件（完整或临时），以临时文件为续传目标
            val resumeFile = when {
                resume && Files.exists(tmp) -> tmp
                resume && Files.exists(target) -> target
                else -> null
            }
            if (resumeFile != null) {
                val size = Files.size(resumeFile)
                if (size > 0L) header("Range", "bytes=$size-")
            }

            block()
        }

        // 选择写入目标（续传写在已有文件上；否则写到 .part）
        val isResumingTo = when {
            resume && Files.exists(tmp) -> tmp
            resume && Files.exists(target) -> target
            else -> tmp
        }

        // 若不续传且文件已存在，按 overwrite 决策
        if (!resume && Files.exists(target) && !overwrite) {
            // 不发请求，构造一个假的响应表示“已存在视为成功”
            return FakeHttpResponse(target, 200)
        }

        // 续传时使用 APPEND；全量下载用 TRUNCATE_EXISTING
        val openOptions = if (resume && Files.exists(isResumingTo)) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        }

        val handler = BodyHandlers.ofFile(isResumingTo, *openOptions)
        val response = request(clientBuilder, requestBuilder, handler)

        // 只有在成功（200/206）时进行原子移动到最终文件
        if (response.statusCode() in 200..299) {
            // 若是写到 .part，则移动；若直接写 target（续传到 target），也统一“再移动覆盖一次”以确保最终一致
            Files.move(isResumingTo, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return FakeHttpResponse(target, response.statusCode(), response)
        } else {
            // 非成功，保留 .part 以便后续手工处理（也可选择删除）
            return FakeHttpResponse(isResumingTo, response.statusCode(), response)
        }
    }

    /**
     * 异步下载到指定文件（原子落盘：临时文件 -> 移动覆盖）
     *
     * @param url            下载链接
     * @param target         目标文件完整路径（不存在会创建父目录）
     * @param resume         是否尝试断点续传（若 target 已存在，则加 Range 头从现有大小续传；服务端需支持 206）
     * @param overwrite      当非续传且目标已存在时是否覆盖；若为 false 且已存在则直接返回 200 且不发请求
     * @param connectTimeout 连接超时（可选）
     * @param block          允许外部继续定制 HttpRequest（加 header、timeout、method 等；默认为 GET+noBody）
     * @return HttpResponse<Path>：body 为最终文件路径，statusCode 可用于判断 200/206/304 等
     */
    fun asyncDownload(
        url: String,
        target: Path,
        resume: Boolean = false,
        overwrite: Boolean = true,
        connectTimeout: Duration? = Duration.ofSeconds(30),
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<Path> {
        // 逻辑与上面一致，只是走 asyncRequest 分支
        Files.createDirectories(target.parent)
        val tmp = target.resolveSibling(target.fileName.toString() + ".part")

        val clientBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
        if (connectTimeout != null) clientBuilder.connectTimeout(connectTimeout)

        val requestBuilder = HttpRequest.newBuilder().apply {
            uri(URI.create(url))
            method("GET", BodyPublishers.noBody())

            val resumeFile = when {
                resume && Files.exists(tmp) -> tmp
                resume && Files.exists(target) -> target
                else -> null
            }
            if (resumeFile != null) {
                val size = Files.size(resumeFile)
                if (size > 0L) header("Range", "bytes=$size-")
            }

            block()
        }

        val isResumingTo = when {
            resume && Files.exists(tmp) -> tmp
            resume && Files.exists(target) -> target
            else -> tmp
        }

        if (!resume && Files.exists(target) && !overwrite) {
            return FakeHttpResponse(target, 200)
        }

        val openOptions = if (resume && Files.exists(isResumingTo)) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        }

        val handler = BodyHandlers.ofFile(isResumingTo, *openOptions)
        val response = asyncRequest(clientBuilder, requestBuilder, handler)

        if (response.statusCode() in 200..299) {
            Files.move(isResumingTo, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return FakeHttpResponse(target, response.statusCode(), response)
        } else {
            return FakeHttpResponse(isResumingTo, response.statusCode(), response)
        }
    }

    /**
     * 自动根据 Content-Disposition 推断文件名；若未提供则从 URL 路径截取。
     *
     * @param url            下载链接
     * @param dir            目标目录
     * @param filename       显式文件名（优先）；为 null 时自动推断
     * @param resume         是否尝试断点续传（若 target 已存在，则加 Range 头从现有大小续传；服务端需支持 206）
     * @param overwrite      当非续传且目标已存在时是否覆盖；若为 false 且已存在则直接返回 200 且不发请求
     * @param connectTimeout 连接超时（可选）
     * @param block          允许外部继续定制 HttpRequest（加 header、timeout、method 等；默认为 GET+noBody）
     * @return HttpResponse<Path>：body 为最终文件路径，statusCode 可用于判断 200/206/304 等
     */
    fun downloadToDir(
        url: String,
        dir: Path,
        filename: String? = null,
        resume: Boolean = false,
        overwrite: Boolean = true,
        connectTimeout: Duration? = Duration.ofSeconds(30),
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<Path> {
        Files.createDirectories(dir)
        // 先发一个 HEAD/GET 仅拿 headers（这里采用 GET，但用 discarding handler）
        val headResp = request<Void>(
            HttpClient.newBuilder(),
            HttpRequest.newBuilder().apply {
                uri(URI.create(url))
                method("GET", BodyPublishers.noBody())
                block()
            },
            BodyHandlers.discarding()
        )

        val name = filename
            ?: parseFilenameFromDisposition(headResp.headers().firstValue("Content-Disposition").orElse(null))
            ?: URI.create(url).path.substringAfterLast('/').ifEmpty { "download.bin" }

        val target = dir.resolve(name)
        return download(url, target, resume, overwrite, connectTimeout, block)
    }

    /** 从 Content-Disposition 萃取 filename/filename*（RFC 5987 简化处理） */
    private fun parseFilenameFromDisposition(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        // filename*=UTF-8''xxx 优先
        val star = Regex("""filename\*\s*=\s*UTF-8''([^;]+)""", RegexOption.IGNORE_CASE)
            .findAll(disposition).firstOrNull()?.groupValues?.getOrNull(1)
        if (!star.isNullOrBlank()) return java.net.URLDecoder.decode(star, StandardCharsets.UTF_8)

        // 退化到 filename="xxx" 或 filename=xxx
        val normal = Regex("""filename\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE)
            .findAll(disposition).firstOrNull()?.groupValues?.getOrNull(1)
        return normal
    }

    /**
     * 一个轻量的 HttpResponse 包装，让我们能够“更改 body 为 Path”并沿用原响应的状态码/头（若有）。
     */
    private class FakeHttpResponse(
        private val path: Path,
        private val code: Int,
        private val delegate: HttpResponse<*>? = null
    ) : HttpResponse<Path> {

        override fun statusCode(): Int = code

        override fun body(): Path = path

        override fun headers(): HttpHeaders =
            delegate?.headers() ?: HttpHeaders.of(emptyMap()) { _, _ -> true }

        override fun request(): HttpRequest =
            delegate?.request() ?: throw UnsupportedOperationException("No underlying request")

        // 关键修正：必须返回 Optional<HttpResponse<Path>>
        override fun previousResponse(): Optional<HttpResponse<Path>> {
            val prevOpt: Optional<out HttpResponse<*>>? = delegate?.previousResponse()
            return if (prevOpt != null && prevOpt.isPresent) {
                val prev = prevOpt.get()
                // 用同一个 path 包一层，保持类型为 HttpResponse<Path>
                Optional.of(FakeHttpResponse(path, prev.statusCode(), prev))
            } else {
                Optional.empty()
            }
        }

        override fun sslSession(): Optional<SSLSession> =
            delegate?.sslSession() ?: Optional.empty()

        override fun uri(): URI =
            delegate?.uri() ?: path.toUri()

        override fun version(): HttpClient.Version =
            delegate?.version() ?: HttpClient.Version.HTTP_1_1
    }


}
