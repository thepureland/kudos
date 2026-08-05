package io.kudos.context.retry

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * test for the IFailedDataHandler default filePath() implementation
 *
 * Coverage:
 * - Without a thread-bound context: falls back to the "default" subdirectory and (crucially)
 *   does NOT implicitly create a KudosContext (the getOrNull contract)
 * - With a context carrying an atomicServiceCode: the code becomes the subdirectory
 *
 * @author K
 * @since 1.0.0
 */
internal class IFailedDataHandlerTest {

    private val handler = object : IFailedDataHandler<String> {
        override val businessType = "bt"
        override val cronExpression = "0 0 * * * *"
        override fun persistFailedData(data: String): String? = null
        override fun handleFailedData(file: File): Boolean = true
    }

    @BeforeTest
    fun clearBefore() = KudosContextHolder.clear()

    @AfterTest
    fun clearAfter() = KudosContextHolder.clear()

    @Test
    fun filePathFallsBackToDefaultWithoutContextAndDoesNotCreateOne() {
        val path = handler.filePath()
        assertTrue(path.endsWith(File.separator + "default"), "missing context should fall back to 'default': $path")
        assertNull(KudosContextHolder.getOrNull(), "filePath must not implicitly create a thread context")
    }

    @Test
    fun filePathUsesAtomicServiceCodeFromContext() {
        KudosContextHolder.set(KudosContext().apply { atomicServiceCode = "svc-42" })
        assertEquals(RetryConfig.pathFor("svc-42"), handler.filePath())
        assertTrue(handler.filePath().endsWith(File.separator + "svc-42"))
    }
}
