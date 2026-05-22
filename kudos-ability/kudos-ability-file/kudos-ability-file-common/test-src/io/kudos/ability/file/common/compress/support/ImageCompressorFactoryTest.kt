package io.kudos.ability.file.common.compress.support

import io.kudos.ability.file.common.compress.compressor.JpgCompressor
import io.kudos.ability.file.common.compress.compressor.PngCompressor
import io.kudos.ability.file.common.compress.compressor.WebPCompressor
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

/**
 * [ImageCompressorFactory] 压缩器选择规则测试。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class ImageCompressorFactoryTest {

    @Test
    fun getCompressor_selectsJpgByExtension() {
        assertIs<JpgCompressor>(ImageCompressorFactory.getCompressor("/tmp/a.jpg", false))
        assertIs<JpgCompressor>(ImageCompressorFactory.getCompressor("/tmp/a.JPEG", false))
    }

    @Test
    fun getCompressor_selectsPngByExtension() {
        assertIs<PngCompressor>(ImageCompressorFactory.getCompressor("/tmp/a.png", false))
    }

    @Test
    fun getCompressor_selectsWebpWhenExtensionIsWebp() {
        assertIs<WebPCompressor>(ImageCompressorFactory.getCompressor("/tmp/a.webp", false))
    }

    @Test
    fun getCompressor_forcesWebpWhenConfigured() {
        assertIs<WebPCompressor>(ImageCompressorFactory.getCompressor("/tmp/a.jpg", true))
        assertIs<WebPCompressor>(ImageCompressorFactory.getCompressor("/tmp/a.txt", true))
    }

    @Test
    fun getCompressor_rejectsUnknownExtension() {
        assertFailsWith<UnsupportedOperationException> {
            ImageCompressorFactory.getCompressor("/tmp/a.txt", false)
        }
    }
}
