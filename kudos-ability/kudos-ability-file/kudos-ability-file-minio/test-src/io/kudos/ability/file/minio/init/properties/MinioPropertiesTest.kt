package io.kudos.ability.file.minio.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Tests for MinIO property configuration.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class MinioPropertiesTest {

    @Test
    fun partSizeDefaultsToTenMiB() {
        val properties = MinioProperties()

        assertEquals(10L * 1024L * 1024L, properties.partSize)
    }

    @Test
    fun partSizeAcceptsConfiguredValue() {
        val properties = MinioProperties()

        properties.partSize = 16L * 1024L * 1024L

        assertEquals(16L * 1024L * 1024L, properties.partSize)
    }

    @Test
    fun partSizeRejectsValuesBelowMinioMinimum() {
        val properties = MinioProperties()

        assertFailsWith<IllegalArgumentException> {
            properties.partSize = MinioProperties.MIN_PART_SIZE - 1
        }
    }

}
