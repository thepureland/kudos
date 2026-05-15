package io.kudos.context.retry

import io.kudos.context.lock.ILeaseLockProvider
import io.kudos.context.lock.NormalLockService
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * FailedDataRetryScanner 测试用例
 *
 * 覆盖：
 * - [FailedDataRetryScanner.retry] 文件扫描：成功删除 / 失败保留 / 异常保留
 * - 完成扫描后空目录自动清理
 * - [FailedDataRetryScanner.lockRetry] 锁交互：取到锁才走 retry，finally 必释放
 *
 * 测试策略：
 * - 内部方法 visible-via-internal，直接调用
 * - 文件操作走 [createTempDirectory]，每个 case 自带清理
 * - 锁注入 [FailedDataRetryScanner.lockProviderSupplier] 绕开 [LockTool] 的 Spring 依赖
 *
 * @author K
 * @since 1.0.0
 */
internal class FailedDataRetryScannerTest {

    private lateinit var tempRoot: Path
    private lateinit var scanner: FailedDataRetryScanner

    @BeforeTest
    fun setup() {
        tempRoot = createTempDirectory("kudos-fdrs-test-")
        scanner = FailedDataRetryScanner()
    }

    @AfterTest
    fun cleanup() {
        // 递归删除整个临时目录
        tempRoot.toFile().walkBottomUp().forEach { it.delete() }
    }

    /** 自定义 handler，把 filePath 指向 tempRoot 下的子目录。 */
    private inner class StubHandler(
        override val businessType: String,
        private val processFn: (StubData) -> Boolean
    ) : IFailedDataHandler<StubData> {

        override val cronExpression: String = "0 0/1 * * * *"

        var processInvocations = 0
            private set

        override fun persistFailedData(data: StubData): String = "ignored"

        override fun handleFailedData(file: File): Boolean {
            processInvocations++
            // 简单地把文件名当 marker，processFn 决定结果
            val data = StubData(file.name)
            return processFn(data)
        }

        override fun filePath(): String = tempRoot.toString()
    }

    private data class StubData(val marker: String)

    /** 在 handler 的业务目录里放一个合法命名的失败数据文件 */
    private fun seedFile(businessType: String, content: String = "{}"): Path {
        val businessDir = tempRoot.resolve(businessType)
        Files.createDirectories(businessDir)
        val name = "${System.currentTimeMillis()}-${UUID.randomUUID()}.json"
        val file = businessDir.resolve(name)
        Files.write(file, content.toByteArray())
        return file
    }

    // ============================================================
    // retry —— 文件扫描与删除
    // ============================================================

    @Test
    fun retryDeletesFileWhenHandlerReturnsTrue() {
        val handler = StubHandler("bizA") { true }
        val file1 = seedFile("bizA")
        val file2 = seedFile("bizA")

        scanner.retry(handler)

        assertEquals(2, handler.processInvocations, "两个文件各处理一次")
        assertFalse(Files.exists(file1), "成功的文件应被删除")
        assertFalse(Files.exists(file2))
        // 业务目录里没文件了应被清理
        assertFalse(Files.exists(tempRoot.resolve("bizA")), "空目录应被清理")
    }

    @Test
    fun retryKeepsFileWhenHandlerReturnsFalse() {
        val handler = StubHandler("bizB") { false }
        val file = seedFile("bizB")

        scanner.retry(handler)

        assertEquals(1, handler.processInvocations)
        assertTrue(Files.exists(file), "失败的文件应保留等待下次重试")
        // 目录还有文件，不应被删
        assertTrue(Files.exists(tempRoot.resolve("bizB")))
    }

    @Test
    fun retrySwallowsHandlerExceptionAndKeepsFile() {
        val handler = StubHandler("bizC") { throw IllegalStateException("processing blew up") }
        val file = seedFile("bizC")

        // 异常不应向上传播——scanner 内部 catch 并打日志
        scanner.retry(handler)

        assertEquals(1, handler.processInvocations)
        assertTrue(Files.exists(file), "异常路径下文件保留")
    }

    @Test
    fun retryHandlesMixedSuccessAndFailure() {
        // handler 偶数次返回 true、奇数次返回 false
        val counter = AtomicInteger(0)
        val handler = StubHandler("bizD") { counter.incrementAndGet() % 2 == 0 }
        val files = (1..4).map { seedFile("bizD") }

        scanner.retry(handler)

        // 文件按文件名排序处理：第 1, 3 次 false 保留，第 2, 4 次 true 删除
        // （具体哪几个被删依赖 sorted() 排序的结果，但总数应是 2 成功 2 失败）
        val remaining = files.count { Files.exists(it) }
        assertEquals(2, remaining, "应有 2 个文件保留（处理失败的）")
    }

    @Test
    fun retryIgnoresUnrelatedFiles() {
        // 不符合 `{millis}-{uuid}.json` 命名模式的文件应被过滤掉
        val businessDir = tempRoot.resolve("bizE")
        Files.createDirectories(businessDir)
        val unrelated = businessDir.resolve("notes.txt")
        Files.write(unrelated, "ignore me".toByteArray())

        val handler = StubHandler("bizE") { true }
        scanner.retry(handler)

        assertEquals(0, handler.processInvocations, "不应处理无关文件")
        assertTrue(Files.exists(unrelated), "无关文件不应被删")
    }

    @Test
    fun retryReturnsImmediatelyWhenDirectoryDoesNotExist() {
        val handler = StubHandler("bizMissing") { true }
        // 不创建 bizMissing 目录
        scanner.retry(handler)
        assertEquals(0, handler.processInvocations, "没目录就没事干")
    }

    // ============================================================
    // lockRetry —— 锁交互
    // ============================================================

    @Test
    fun lockRetryRunsRetryWhenLockAcquired() {
        val handler = StubHandler("bizLock") { true }
        seedFile("bizLock")

        // 注入一个全新的本地锁服务，确保 tryLock 必定成功
        scanner.lockProviderSupplier = { NormalLockService() }

        scanner.lockRetry(handler, appName = "test-svc")

        assertEquals(1, handler.processInvocations, "拿到锁后应执行 retry")
    }

    @Test
    fun lockRetrySkipsRetryWhenLockBusy() {
        val handler = StubHandler("bizBusy") { true }
        seedFile("bizBusy")

        // 一个永远获取失败的 stub lock
        scanner.lockProviderSupplier = {
            object : ILeaseLockProvider {
                override fun tryLock(lockKey: String, sec: Int): Boolean = false
                override fun unLock(key: String) {
                    error("lockBusy 测试中不应该 unLock，因为 tryLock 失败")
                }
            }
        }

        scanner.lockRetry(handler, appName = "test-svc")

        assertEquals(0, handler.processInvocations, "拿不到锁就不应执行 retry")
    }

    @Test
    fun lockRetryReleasesLockEvenIfRetryThrows() {
        // 当 retry 中 handler 抛异常时，scanner 内部 catch 不外抛，所以 lockRetry 不会异常退出
        // 这条测试保证：unLock 一定被调用（finally 路径生效）
        val unlockCalled = AtomicBoolean(false)
        val handler = StubHandler("bizThrow") { throw RuntimeException("boom") }
        seedFile("bizThrow")

        scanner.lockProviderSupplier = {
            object : ILeaseLockProvider {
                override fun tryLock(lockKey: String, sec: Int): Boolean = true
                override fun unLock(key: String) {
                    unlockCalled.set(true)
                }
            }
        }

        scanner.lockRetry(handler, appName = "test-svc")

        assertTrue(unlockCalled.get(), "即便 handler 抛异常也必须释放锁（finally）")
    }
}
