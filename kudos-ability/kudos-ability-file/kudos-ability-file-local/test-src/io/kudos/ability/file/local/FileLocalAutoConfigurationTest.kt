package io.kudos.ability.file.local

import io.kudos.ability.file.local.init.FileLocalAutoConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertNull

/**
 * Test cases for [FileLocalAutoConfiguration] bean factory methods and component metadata.
 *
 * Coverage:
 * - each @Bean factory method returns a new, non-null instance
 * - localProperties() defaults (basePath is null before binding)
 * - getComponentName() returns the fixed component id
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class FileLocalAutoConfigurationTest {

    private val configuration = FileLocalAutoConfiguration()

    @Test
    fun localUploadService_creates_new_instance() {
        assertNotNull(configuration.localUploadService())
        assertNotSame(configuration.localUploadService(), configuration.localUploadService())
    }

    @Test
    fun localDownLoadService_creates_new_instance() {
        assertNotNull(configuration.localDownLoadService())
        assertNotSame(configuration.localDownLoadService(), configuration.localDownLoadService())
    }

    @Test
    fun localDeleteService_creates_new_instance() {
        assertNotNull(configuration.localDeleteService())
        assertNotSame(configuration.localDeleteService(), configuration.localDeleteService())
    }

    @Test
    fun localProperties_defaults_to_null_basePath() {
        val properties = configuration.localProperties()
        assertNotNull(properties)
        assertNull(properties.basePath)
    }

    @Test
    fun getComponentName_returns_fixed_id() {
        assertEquals("kudos-ability-file-local", configuration.getComponentName())
    }

}
