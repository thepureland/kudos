package io.kudos.base.net.http

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.io.IOException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for HttpClientKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HttpClientKitTest {

    private lateinit var server: HttpServer
    private var baseUrl: String = ""
    private val pool = Executors.newFixedThreadPool(4)

    private lateinit var pngBytes: ByteArray

    private lateinit var tmpDir: Path



    @Serializable
    data class Foo(val id: Int, val name: String)

    @Serializable
    data class Wrapper<T>(val code: Int, val data: T)

    @BeforeAll
    fun start_server() {
        // 读取测试资源 kudos.png（放在 test resources 根目录）
        val url = this::class.java.getResource("/logo.png")
            ?: error("测试资源 /logo.png 未找到，请确保放在 test resources 下")
        pngBytes = url.readBytes()

        // 启动 JDK 内置 HttpServer（随机端口，避免冲突）
        server = HttpServer.create(InetSocketAddress(0), 0)

        // GET /text -> "hello"
        server.createContext("/text") { ex ->
            ex.respond(200, "hello", "text/plain; charset=utf-8")
        }

        // GET /bytes -> [1,2,3]
        server.createContext("/bytes") { ex ->
            ex.respondBytes(200, byteArrayOf(1, 2, 3), "application/octet-stream")
        }

        // GET /stream -> "stream-body"
        server.createContext("/stream") { ex ->
            ex.respondBytes(200, "stream-body".toByteArray(StandardCharsets.UTF_8), "application/octet-stream")
        }

        // GET /json -> {"code":0,"data":{"id":1,"name":"kudos"}}
        server.createContext("/json") { ex ->
            val body = """{"code":0,"data":{"id":1,"name":"kudos"}}"""
            ex.respond(200, body, "application/json; charset=utf-8")
        }
        server.createContext("/json-invalid") { ex ->
            ex.respond(200, "not-json", "application/json; charset=utf-8")
        }

        // GET /arrayByte -> [1,2,3]
        server.createContext("/arrayByte") { ex ->
            ex.respondBytes(200, byteArrayOf(1, 2, 3), "application/octet-stream")
        }

        // GET /empty -> 204
        server.createContext("/empty") { ex ->
            ex.sendResponseHeaders(204, -1)
            ex.close()
        }

        // 任意方法：回显方法名
        server.createContext("/method-echo") { ex ->
            ex.respond(200, ex.requestMethod, "text/plain; charset=utf-8")
        }

        // 表单回显
        server.createContext("/form-echo") { ex ->
            val body = ex.requestBody.readAllBytes()
            ex.respondBytes(200, body, "text/plain; charset=utf-8")
        }

        // 200 全量下载
        server.createContext("/logo.png") { ex ->
            ex.respond_ok_full(pngBytes, "image/png")
        }

        // 206 断点续传（仅处理单一 Range: bytes=start- 或 bytes=start-end）
        server.createContext("/kudos-range") { ex ->
            ex.respond_with_range(pngBytes, "image/png")
        }

        // 200 + Content-Disposition（用于自动文件名）
        server.createContext("/kudos-cd") { ex ->
            ex.responseHeaders.add("Content-Type", "image/png")
            ex.responseHeaders.add("Content-Disposition", """attachment; filename="kudos-from-header.png"""")
            ex.sendResponseHeaders(200, pngBytes.size.toLong())
            ex.responseBody.use { it.write(pngBytes) }
            ex.close()
        }

        server.executor = pool
        server.start()
        val port = server.address.port
        baseUrl = "http://127.0.0.1:$port"
        println("Test server started at $baseUrl")

        // 临时目录
        tmpDir = Files.createTempDirectory("dl-test-")
    }

    @AfterAll
    fun stop_server() {
        server.stop(0)
        pool.shutdown()
        tmpDir.toFile().deleteRecursively()
    }

    private fun url(path: String) = "$baseUrl$path"

    /* =========================
       同步请求：GET，基础类型
       ========================= */

    @Test
    fun get_string() {
        val resp = HttpClientKit.get<String>(url("/text"))
        assertEquals(200, resp.statusCode())
        assertEquals("hello", resp.body())
    }

    @Test
    fun get_byte_array() {
        val resp = HttpClientKit.get<ByteArray>(url("/bytes"))
        assertEquals(200, resp.statusCode())
        assertContentEquals(byteArrayOf(1, 2, 3), resp.body())
    }

    @Test
    fun get_input_stream() {
        val resp = HttpClientKit.get<java.io.InputStream>(url("/stream"))
        assertEquals(200, resp.statusCode())
        val bytes = resp.body().readAllBytes()
        assertContentEquals("stream-body".toByteArray(), bytes)
    }

    @Test
    fun get_array_byte() {
        val resp = HttpClientKit.get<Array<Byte>>(url("/arrayByte"))
        assertEquals(200, resp.statusCode())
        assertContentEquals(arrayOf(1, 2, 3), resp.body().toList().toTypedArray())
    }

    @Test
    fun get_byte_buffer() {
        val resp = HttpClientKit.get<ByteBuffer>(url("/bytes"))
        assertEquals(200, resp.statusCode())
        val body = resp.body()
        val bytes = ByteArray(body.remaining()).also { body.get(it) }
        assertContentEquals(byteArrayOf(1, 2, 3), bytes)
    }

    @Test
    fun get_unit_no_body() {
        val resp = HttpClientKit.get<Unit>(url("/empty"))
        assertEquals(204, resp.statusCode())
        assertEquals(Unit, resp.body())
    }

    /* =========================
       同步请求：默认 JSON 映射
       ========================= */

    @Test
    fun get_default_json_mapping_to_generic_type() {
        val resp = HttpClientKit.get<Wrapper<Foo>>(url("/json"))
        assertEquals(200, resp.statusCode())
        val body = resp.body()
        assertNotNull(body)
        assertEquals(0, body.code)
        assertEquals(1, body.data.id)
        assertEquals("kudos", body.data.name)
    }

    @Test
    fun default_json_mapping_should_fail_for_invalid_json() {
        assertFailsWith<IOException> {
            HttpClientKit.get<Wrapper<Foo>>(url("/json-invalid"))
        }
    }

    /* =========================
       异步请求：方法与表单
       ========================= */
    @Test
    fun async_post_with_form_data() {
        val form: Map<Any, Any> = mapOf("a b" to "中 文", "x" to "1+2=3")
        val resp = HttpClientKit.asyncPost<String>(url("/form-echo")) {
            header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            // 关键：直接传 Publisher，不要做任何类型转换或 toString()
            method("POST", HttpClientKit.ofFormData(form))
        }
        assertEquals(200, resp.statusCode())
        // 服务器回显请求体，断言编码结果
        val expected = "a+b=%E4%B8%AD+%E6%96%87&x=1%2B2%3D3"
        assertEquals(expected, resp.body())
    }

    @Test
    fun async_get_method_echo() {
        val resp = HttpClientKit.asyncGet<String>(url("/method-echo"))
        assertEquals(200, resp.statusCode())
        assertEquals("GET", resp.body())
    }

    @Test
    fun put_method_works() {
        val resp = HttpClientKit.put<String>(url("/method-echo")) {
            method("PUT", java.net.http.HttpRequest.BodyPublishers.noBody())
        }
        assertEquals(200, resp.statusCode())
        assertEquals("PUT", resp.body())
    }

    @Test
    fun delete_method_works() {
        val resp = HttpClientKit.delete<String>(url("/method-echo")) {
            method("DELETE", java.net.http.HttpRequest.BodyPublishers.noBody())
        }
        assertEquals(200, resp.statusCode())
        assertEquals("DELETE", resp.body())
    }

    @Test
    fun options_method_works() {
        val resp = HttpClientKit.options<String>(url("/method-echo")) {
            method("OPTIONS", java.net.http.HttpRequest.BodyPublishers.noBody())
        }
        assertEquals(200, resp.statusCode())
        assertEquals("OPTIONS", resp.body())
    }

    @Test
    fun trace_method_works() {
        val resp = HttpClientKit.trace<String>(url("/method-echo")) {
            method("TRACE", java.net.http.HttpRequest.BodyPublishers.noBody())
        }
        assertEquals(200, resp.statusCode())
        assertEquals("TRACE", resp.body())
    }

    @Test
    fun patch_method_works() {
        val resp = HttpClientKit.patch<String>(url("/method-echo")) {
            method("PATCH", java.net.http.HttpRequest.BodyPublishers.ofString("patch"))
        }
        assertEquals(200, resp.statusCode())
        assertEquals("PATCH", resp.body())
    }

    @Test
    fun download_full_file_to_target() {
        val target = tmpDir.resolve("kudos_full.png")
        val resp = HttpClientKit.download(
            url = url("/logo.png"),
            target = target,
            resume = false,
            overwrite = true
        )
        assertEquals(200, resp.statusCode())
        assertTrue(target.exists(), "文件应已落盘")
        assertContentEquals(pngBytes, target.readBytes(), "下载内容应与资源一致")
    }

    @Test
    fun async_download_full_file() {
        val target = tmpDir.resolve("kudos_async.png")
        val resp = HttpClientKit.asyncDownload(
            url = url("/logo.png"),
            target = target,
            resume = false,
            overwrite = true
        )
        assertEquals(200, resp.statusCode())
        assertTrue(target.exists())
        assertContentEquals(pngBytes, target.readBytes())
    }

    @Test
    fun download_to_dir_auto_filename_via_content_disposition() {
        val dir = tmpDir.resolve("auto").apply { createDirectories() }
        val resp = HttpClientKit.downloadToDir(
            url = url("/kudos-cd"),
            dir = dir
        )
        assertEquals(200, resp.statusCode())
        val saved = resp.body()
        assertTrue(saved.fileName.toString() == "kudos-from-header.png", "应根据 Content-Disposition 自动取名")
        assertContentEquals(pngBytes, saved.readBytes())
    }

    @Test
    fun resume_download_from_existing_part_file() {
        // 先人工制造一个 .part（写入前半段）
        val target = tmpDir.resolve("kudos_partial.png")
        val part = target.resolveSibling(target.name + ".part")
        val half = (pngBytes.size / 2).coerceAtLeast(1)
        part.writeBytes(pngBytes.copyOfRange(0, half))

        // 继续下载（服务器支持 Range）
        val resp = HttpClientKit.download(
            url = url("/kudos-range"),
            target = target,
            resume = true,
            overwrite = true
        )
        // 续传时服务端返回 206，这里沿用底层状态码
        assertEquals(206, resp.statusCode(), "续传应返回 206 Partial Content")
        assertTrue(target.exists())
        assertContentEquals(pngBytes, target.readBytes(), "最终文件应与原文件一致")
    }

    @Test
    fun download_should_not_overwrite_when_overwrite_false() {
        val target = tmpDir.resolve("kudos_no_overwrite.png")
        val sentinel = "already-here".toByteArray(StandardCharsets.UTF_8)
        target.writeBytes(sentinel)

        val resp = HttpClientKit.download(
            url = url("/kudos.png"),
            target = target,
            resume = false,
            overwrite = false
        )
        // 我们的实现：当 overwrite=false 且文件存在时，不发请求，直接返回 200 并保持原文件
        assertEquals(200, resp.statusCode())
        assertContentEquals(sentinel, target.readBytes(), "不应覆盖已存在文件")
    }

    /* =========================
       注意事项与已知差异
       ========================= */

    @Test
    fun unit_and_byte_buffer_behavior_documented_by_real_assertions() {
        val unitResp = HttpClientKit.get<Unit>(url("/empty"))
        assertEquals(Unit, unitResp.body())
        val bufferResp = HttpClientKit.get<ByteBuffer>(url("/bytes"))
        assertEquals(3, bufferResp.body().remaining())
    }

    /* =========================
       HttpExchange 扩展与工具
       ========================= */

    private fun HttpExchange.respond(status: Int, text: String, contentType: String) {
        val bytes = text.toByteArray(StandardCharsets.UTF_8)
        responseHeaders.add("Content-Type", contentType)
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
        close()
    }

    private fun HttpExchange.respondBytes(status: Int, bytes: ByteArray, contentType: String) {
        responseHeaders.add("Content-Type", contentType)
        sendResponseHeaders(status, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
        close()
    }


    // 200 全量
    private fun HttpExchange.respond_ok_full(bytes: ByteArray, contentType: String) {
        responseHeaders.add("Content-Type", contentType)
        responseHeaders.add("Accept-Ranges", "bytes")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
        close()
    }

    // 支持 Range 的响应（只实现单一 range）
    private fun HttpExchange.respond_with_range(bytes: ByteArray, contentType: String) {
        responseHeaders.add("Content-Type", contentType)
        responseHeaders.add("Accept-Ranges", "bytes")
        val range = requestHeaders.getFirst("Range")
        if (range == null) {
            // 无 Range，当作全量
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
            close()
            return
        }

        // 解析 Range: bytes=start-end?  （只处理单段）
        val m = Regex("""bytes=(\d+)-(\d+)?""").find(range)
        if (m == null) {
            // 不合法的 Range，简单回 200 全量
            sendResponseHeaders(200, bytes.size.toLong())
            responseBody.use { it.write(bytes) }
            close()
            return
        }

        val start = m.groupValues[1].toLong()
        val endIncl = m.groupValues[2].takeIf { it.isNotEmpty() }?.toLong() ?: (bytes.size - 1).toLong()
        val clampedStart = start.coerceIn(0, (bytes.size - 1).toLong())
        val clampedEnd = endIncl.coerceIn(clampedStart, (bytes.size - 1).toLong())

        val slice = bytes.copyOfRange(clampedStart.toInt(), (clampedEnd + 1).toInt())
        responseHeaders.add("Content-Range", "bytes $clampedStart-$clampedEnd/${bytes.size}")
        sendResponseHeaders(206, slice.size.toLong())
        responseBody.use { it.write(slice) }
        close()
    }

}
