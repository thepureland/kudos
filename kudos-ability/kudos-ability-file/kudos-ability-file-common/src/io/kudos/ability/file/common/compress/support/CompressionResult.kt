package io.kudos.ability.file.common.compress.support

import io.kudos.base.logger.LogFactory
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

/**
 * Image compression result. Wraps the in-memory [outputStream], the target file path,
 * and the final MIME type.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CompressionResult(val outputStream: ByteArrayOutputStream?, var mimeType: String?) {
    private var outputFilePath: String? = null

    constructor(outputStream: ByteArrayOutputStream?, outputFilePath: String, mimeType: String?) : this(
        outputStream,
        mimeType
    ) {
        this.outputFilePath = outputFilePath
    }

    fun getOutputFilePath(): String {
        return requireNotNull(outputFilePath) { "outputFilePath is null" }
    }

    /**
     * Flushes the in-memory compression result to disk at [outputFilePath].
     *
     * Historical issue: the old implementation `catch (ignored: Exception) { }`
     * silently swallowed all IO errors — when compression failed, the caller had
     * no idea, and the file may not have been written successfully at all. Changed
     * to log error to make incidents visible. We still do not rethrow in order to
     * stay compatible with the old caller
     * ([io.kudos.ability.file.common.compress.CompressionPipeline.compressAndOutputFile]
     * does not declare IOException); switching to throw can be considered later.
     */
    fun writeTo() {
        if (outputStream != null) {
            try {
                FileOutputStream(outputFilePath).use { out ->
                    outputStream.writeTo(out)
                }
            } catch (e: Exception) {
                log.error(e, "Failed to write image compression result to file: path={0}", outputFilePath)
            }
        }
    }

    companion object {
        private val log = LogFactory.getLog(CompressionResult::class)
    }
}
