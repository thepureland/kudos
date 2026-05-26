package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.InputStream

/**
 * Image compression protocol.
 *
 * [io.kudos.ability.file.common.compress.support.ImageCompressorFactory] selects the
 * appropriate implementation (Jpg / Png / WebP) based on [CompressionConfig]. When
 * adding a new compression format, implement this interface and register it in the
 * factory.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface ImageCompressor {

    /**
     * Determines whether the current compressor handles this task.
     * For example, [WebPCompressor] only returns true when `config.webp = true`,
     * letting the upper layer skip WebP in plain image scenarios.
     *
     * @param config compression configuration
     * @return true if this compressor handles the task
     */
    fun support(config: CompressionConfig): Boolean

    /**
     * Performs compression.
     *
     * @param inputStream source image input stream (the caller is responsible for closing it)
     * @param destination expected target path (concrete implementations may rename, e.g. WebP appends `.webp`)
     * @param config compression configuration
     * @return result containing the compressed byte stream, final path, and Content-Type
     */
    fun compress(inputStream: InputStream, destination: String, config: CompressionConfig): CompressionResult

}
