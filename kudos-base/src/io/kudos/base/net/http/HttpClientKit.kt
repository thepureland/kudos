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
import java.util.Optional
import javax.net.ssl.SSLSession
import kotlin.reflect.typeOf


/**
 * HTTP Client utility class.
 *
 * A lightweight wrapper around Java 11+ `java.net.http.HttpClient`, providing:
 * - synchronous / asynchronous (coroutine-await) request capability
 * - automatic response body deserialization based on **reified generics**
 *   (String/ByteArray/InputStream/Unit/custom JSON, etc.)
 * - convenient methods for common HTTP verbs (GET/POST/PUT/DELETE/OPTIONS/CONNECT/TRACE/PATCH)
 *
 * Notes:
 * - The default branch parses the response body as **JSON** into `T` (using `JsonKit`), so if the response is
 *   not JSON, explicitly specify `T` as a concrete type (e.g. String/ByteArray) at the call site.
 * - The `ByteBuffer` branch maps the bytes to `ByteBuffer.wrap(bytes)` and returns it.
 *
 * @author K
 * @author AI: ChatGPT
 * @since 1.0.0
 */
object HttpClientKit {

    // -------------- Convenience methods (sync/async + common HTTP verbs) --------------

    /** Synchronous GET; T determines the response body type at the call site. */
    inline fun <reified T : Any> get(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "GET", block)
    }

    /** Asynchronous GET; internally uses a coroutine to await the sendAsync result and return it. */
    inline fun <reified T : Any> asyncGet(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest(url, "GET", block)
    }

    /** Synchronous POST. */
    inline fun <reified T : Any> post(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "POST", block)
    }

    /** Asynchronous POST. */
    inline fun <reified T : Any> asyncPost(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "POST", block)
    }

    /** Synchronous PUT. */
    inline fun <reified T : Any> put(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "PUT", block)
    }

    /** Asynchronous PUT. */
    inline fun <reified T : Any> asyncPut(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "PUT", block)
    }

    /** Synchronous DELETE. */
    inline fun <reified T : Any> delete(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "DELETE", block)
    }

    /** Asynchronous DELETE. */
    inline fun <reified T : Any> asyncDelete(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "DELETE", block)
    }

    /** Synchronous OPTIONS. */
    inline fun <reified T : Any> options(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "OPTIONS", block)
    }

    /** Asynchronous OPTIONS. */
    inline fun <reified T : Any> asyncOptions(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "OPTIONS", block)
    }

    /** Synchronous CONNECT. */
    inline fun <reified T : Any> connect(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "CONNECT", block)
    }

    /** Asynchronous CONNECT. */
    inline fun <reified T : Any> asyncConnect(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "CONNECT", block)
    }

    /** Synchronous TRACE. */
    inline fun <reified T : Any> trace(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "TRACE", block)
    }

    /** Asynchronous TRACE. */
    inline fun <reified T : Any> asyncTrace(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "TRACE", block)
    }

    /** Synchronous PATCH. */
    inline fun <reified T : Any> patch(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return request<T>(url, "PATCH", block)
    }

    /** Asynchronous PATCH. */
    inline fun <reified T : Any> asyncPatch(url: String, block: HttpRequest.Builder.() -> Unit = {}): HttpResponse<T> {
        return asyncRequest<T>(url, "PATCH", block)
    }

    // -------------- Unified sync/async entry points (URL + method as input) --------------

    /**
     * Synchronous request entry point: builds the request from URL + HTTP method.
     * @param block may append headers, timeout, body, etc. on the builder (noBody is set by default and can be
     *              overridden in block)
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
        val bodyHandler = __createBodyHandler<T>()      // derive BodyHandler from T
        return request(clientBuilder, requestBuilder, bodyHandler)
    }

    /**
     * Asynchronous request entry point (coroutine await): same as the synchronous version above, except finally
     * uses sendAsync + await.
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

    // -------------- Low-level execution (reuse the passed-in Builder/Handler for finer control) --------------

    /**
     * Issues an asynchronous request via coroutines (does not block the thread) and awaits the result.
     *
     * Conventions:
     * - Launches a child coroutine on `Dispatchers.IO` to avoid blocking the main thread / test thread.
     * - `sendAsync(...).await()` leverages the `kotlinx-coroutines-jdk8` extension for `CompletableFuture`.
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
     * Synchronous execution: directly calls `HttpClient.send(...)`.
     * - Suitable for calls with simple timing requirements or no concurrency needs.
     */
    fun <T : Any> request(
        httpClientBuilder: HttpClient.Builder,
        httpRequestBuilder: HttpRequest.Builder,
        bodyHandler: BodyHandler<T>
    ): HttpResponse<T> {
        val client = createHttpClient(httpClientBuilder) // `.build()` is factored out for future injection/replacement
        val request = httpRequestBuilder.build()
        return client.send(request, bodyHandler)
    }

    /**
     * Actual asynchronous execution logic:
     * - build the request
     * - client.sendAsync(request, handler)
     * - await with `await()`
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

    /**
     * Converts a [HttpRequest.Builder] into an immutable [HttpRequest].
     * Extracted as a separate function mainly to facilitate future extensions such as adding instrumentation
     * or default headers; currently a thin wrapper.
     *
     * @param httpRequestBuilder the request builder
     * @return the built request
     * @author K
     * @since 1.0.0
     */
    private fun createHttpRequest(httpRequestBuilder: HttpRequest.Builder): HttpRequest {
        return httpRequestBuilder.build()
    }

    /**
     * Converts a [HttpClient.Builder] into an [HttpClient].
     * Extracted as a separate function to facilitate future replacement with an injectable client factory
     * (e.g. unified injection of connection pools or certificate-validation policies).
     *
     * @param httpClientBuilder the client builder
     * @return the built client
     * @author K
     * @since 1.0.0
     */
    private fun createHttpClient(httpClientBuilder: HttpClient.Builder): HttpClient {
        return httpClientBuilder.build()
    }

    // -------------- BodyHandler selector based on reified T --------------

    /**
     * Dynamically selects a `BodyHandler<T>` based on the reified `T` at the call site.
     *
     * Supported:
     * - String              -> `BodyHandlers.ofString()`
     * - ByteArray           -> `BodyHandlers.ofByteArray()`
     * - InputStream         -> `BodyHandlers.ofInputStream()`
     * - ByteBuffer          -> `BodyHandlers.ofByteArray()` then mapped to `ByteBuffer.wrap(bytes)`
     * - Void / Unit         -> `BodyHandlers.discarding()` (no-body scenarios, typically 204; only care about status/headers)
     * - Array<Byte>         -> maps the result of `ofByteArray()` to `Array<Byte>`
     * - Other types (default) -> takes raw bytes then parses them as a JSON object/collection/generic via `JsonKit.fromJson<T>()`
     *
     * Risks/Notes:
     * - Default JSON branch: ensure the response is valid JSON and that `JsonKit` can correctly parse it into `T`
     *   (including complex generics).
     */
    inline fun <reified T: Any> __createBodyHandler() : BodyHandler<T> {
        val kType = typeOf<T>()
        return when (kType) {
            typeOf<String>() -> BodyHandler { responseInfo ->
                BodySubscribers.mapping(BodyHandlers.ofString().apply(responseInfo)) { body ->
                    T::class.java.cast(body)
                }
            }
            typeOf<ByteArray>() -> BodyHandler { responseInfo ->
                BodySubscribers.mapping(BodyHandlers.ofByteArray().apply(responseInfo)) { body ->
                    T::class.java.cast(body)
                }
            }
            typeOf<InputStream>() -> BodyHandler { responseInfo ->
                BodySubscribers.mapping(BodyHandlers.ofInputStream().apply(responseInfo)) { body ->
                    T::class.java.cast(body)
                }
            }
            typeOf<ByteBuffer>() -> BodyHandler { responseInfo ->
                BodySubscribers.mapping(BodyHandlers.ofByteArray().apply(responseInfo)) { bytes ->
                    T::class.java.cast(ByteBuffer.wrap(bytes))
                }
            }
            typeOf<Void>() -> BodyHandler { _ ->
                BodySubscribers.mapping(BodySubscribers.discarding()) {
                    T::class.java.cast(null)
                }
            }
            typeOf<Unit>() -> BodyHandler { _ ->
                BodySubscribers.mapping(BodySubscribers.discarding()) {
                    T::class.java.cast(Unit)
                }
            }
            typeOf<Array<Byte>>() -> BodyHandler {
                BodySubscribers.mapping(BodySubscribers.ofByteArray()) { bytes ->
                    T::class.java.cast(bytes.toTypedArray())           // convert the raw ByteArray to Array<Byte>
                }
            }
            else -> {
                // Default JSON handling (supports complex generics)
                BodyHandler {
                    BodySubscribers.mapping(BodySubscribers.ofByteArray()) { bytes ->
                        JsonKit.fromJson<T>(bytes.toString(StandardCharsets.UTF_8))
                            ?: error("Failed to deserialize HTTP response JSON: ${T::class.qualifiedName}")
                    }
                }
            }
        }
    }

    // -------------- BodyPublisher construction for x-www-form-urlencoded --------------

    /**
     * Encodes a key-value Map into a `BodyPublisher` using the `application/x-www-form-urlencoded` rules.
     *
     * Conventions/Details:
     * - Encodes both key and value with `URLEncoder.encode(..., UTF-8)`.
     * - Key-value pairs are joined with `&`; key and value are joined with `=`.
     * - This method **only returns** the request body; it does not automatically set the `Content-Type` header.
     *   Callers typically need: `header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")`
     *
     * Example output:
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
        return BodyPublishers.ofString(builder.toString())              // produce a string request body
    }


    /**
     * Synchronously downloads to the specified file (atomic write: temp file -> move-overwrite).
     *
     * @param url            the download URL
     * @param target         the full target file path (parent directories are created if missing)
     * @param resume         whether to attempt resumable download (if target exists, sends a Range header to resume
     *                        from the existing size; the server must support 206)
     * @param overwrite      when not resuming and the target already exists, whether to overwrite; if false and the
     *                        target already exists, returns 200 directly without sending a request
     * @param connectTimeout connection timeout (optional)
     * @param block          allows the caller to further customize the HttpRequest (headers, timeout, method, etc.;
     *                        defaults to GET+noBody)
     * @return HttpResponse<Path>: body is the final file path; statusCode can be used to check 200/206/304, etc.
     */
    fun download(
        url: String,
        target: Path,
        resume: Boolean = false,
        overwrite: Boolean = true,
        connectTimeout: Duration? = Duration.ofSeconds(30),
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<Path> {
        // Prepare directories and the temporary file
        Files.createDirectories(target.parent)
        val tmp = target.resolveSibling(target.fileName.toString() + ".part")

        val clientBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
        connectTimeout?.let { clientBuilder.connectTimeout(it) }

        val requestBuilder = HttpRequest.newBuilder().apply {
            uri(URI.create(url))
            method("GET", BodyPublishers.noBody())

            // Resume: if an existing file (complete or temp) is present, use the temp file as the resume target
            val resumeFile = when {
                resume && Files.exists(tmp) -> tmp
                resume && Files.exists(target) -> target
                else -> null
            }
            resumeFile?.let {
                val size = Files.size(it)
                if (size > 0L) header("Range", "bytes=$size-")
            }

            block()
        }

        // Choose the write target (when resuming, write to the existing file; otherwise write to .part)
        val isResumingTo = when {
            resume && Files.exists(tmp) -> tmp
            resume && Files.exists(target) -> target
            else -> tmp
        }

        // If not resuming and the file already exists, decide based on overwrite
        if (!resume && Files.exists(target) && !overwrite) {
            // Skip the request and build a fake response indicating "exists treated as success"
            return FakeHttpResponse(target, 200)
        }

        // Use APPEND when resuming; use TRUNCATE_EXISTING for full downloads
        val openOptions = if (resume && Files.exists(isResumingTo)) {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE)
        } else {
            arrayOf(StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)
        }

        val handler = BodyHandlers.ofFile(isResumingTo, *openOptions)
        val response = request(clientBuilder, requestBuilder, handler)

        // Only perform the atomic move to the final file on success (200/206)
        if (response.statusCode() in 200..299) {
            // If written to .part, move it; if written directly to target (resumed into target), also perform a
            // uniform "move-overwrite again" to ensure final consistency.
            Files.move(isResumingTo, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            return FakeHttpResponse(target, response.statusCode(), response)
        } else {
            // On failure, keep .part for later manual handling (deletion is also an option)
            return FakeHttpResponse(isResumingTo, response.statusCode(), response)
        }
    }

    /**
     * Asynchronously downloads to the specified file (atomic write: temp file -> move-overwrite).
     *
     * @param url            the download URL
     * @param target         the full target file path (parent directories are created if missing)
     * @param resume         whether to attempt resumable download (if target exists, sends a Range header to resume
     *                        from the existing size; the server must support 206)
     * @param overwrite      when not resuming and the target already exists, whether to overwrite; if false and the
     *                        target already exists, returns 200 directly without sending a request
     * @param connectTimeout connection timeout (optional)
     * @param block          allows the caller to further customize the HttpRequest (headers, timeout, method, etc.;
     *                        defaults to GET+noBody)
     * @return HttpResponse<Path>: body is the final file path; statusCode can be used to check 200/206/304, etc.
     */
    fun asyncDownload(
        url: String,
        target: Path,
        resume: Boolean = false,
        overwrite: Boolean = true,
        connectTimeout: Duration? = Duration.ofSeconds(30),
        block: HttpRequest.Builder.() -> Unit = {}
    ): HttpResponse<Path> {
        // Same logic as above, but goes through the asyncRequest branch
        Files.createDirectories(target.parent)
        val tmp = target.resolveSibling(target.fileName.toString() + ".part")

        val clientBuilder = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
        connectTimeout?.let { clientBuilder.connectTimeout(it) }

        val requestBuilder = HttpRequest.newBuilder().apply {
            uri(URI.create(url))
            method("GET", BodyPublishers.noBody())

            val resumeFile = when {
                resume && Files.exists(tmp) -> tmp
                resume && Files.exists(target) -> target
                else -> null
            }
            resumeFile?.let {
                val size = Files.size(it)
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
     * Infers the filename automatically from Content-Disposition; if not provided, derives it from the URL path.
     *
     * @param url            the download URL
     * @param dir            the target directory
     * @param filename       explicit filename (takes precedence); auto-inferred when null
     * @param resume         whether to attempt resumable download (if target exists, sends a Range header to resume
     *                        from the existing size; the server must support 206)
     * @param overwrite      when not resuming and the target already exists, whether to overwrite; if false and the
     *                        target already exists, returns 200 directly without sending a request
     * @param connectTimeout connection timeout (optional)
     * @param block          allows the caller to further customize the HttpRequest (headers, timeout, method, etc.;
     *                        defaults to GET+noBody)
     * @return HttpResponse<Path>: body is the final file path; statusCode can be used to check 200/206/304, etc.
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
        // First issue a HEAD/GET just to grab the headers (here we use GET with a discarding handler)
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

    /** Extracts filename/filename* from Content-Disposition (simplified RFC 5987 handling). */
    private fun parseFilenameFromDisposition(disposition: String?): String? {
        if (disposition.isNullOrBlank()) return null
        // filename*=UTF-8''xxx takes precedence
        val star = Regex("""filename\*\s*=\s*UTF-8''([^;]+)""", RegexOption.IGNORE_CASE)
            .findAll(disposition).firstOrNull()?.groupValues?.getOrNull(1)
        if (!star.isNullOrBlank()) return java.net.URLDecoder.decode(star, StandardCharsets.UTF_8)

        // Fall back to filename="xxx" or filename=xxx
        val normal = Regex("""filename\s*=\s*"?([^";]+)"?""", RegexOption.IGNORE_CASE)
            .findAll(disposition).firstOrNull()?.groupValues?.getOrNull(1)
        return normal
    }

    /**
     * A lightweight HttpResponse wrapper that lets us "change the body to a Path" while reusing the original
     * response's status code/headers (if any).
     */
    private class FakeHttpResponse(
        /** The body, replaced with the final on-disk path. */
        private val path: Path,
        /** Custom status code, e.g. 200 when an already-existing local file is a cache hit. */
        private val code: Int,
        /** Original response; used to proxy headers/uri/version metadata. May be null (when constructing a fake response). */
        private val delegate: HttpResponse<*>? = null
    ) : HttpResponse<Path> {

        override fun statusCode(): Int = code

        override fun body(): Path = path

        override fun headers(): HttpHeaders =
            delegate?.headers() ?: HttpHeaders.of(emptyMap()) { _, _ -> true }

        override fun request(): HttpRequest =
            delegate?.request() ?: throw UnsupportedOperationException("No underlying request")

        // Key fix: must return Optional<HttpResponse<Path>>
        override fun previousResponse(): Optional<HttpResponse<Path>> {
            val prevOpt: Optional<out HttpResponse<*>>? = delegate?.previousResponse()
            return if (prevOpt != null && prevOpt.isPresent) {
                val prev = prevOpt.get()
                // Wrap with the same path to preserve the HttpResponse<Path> type
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
