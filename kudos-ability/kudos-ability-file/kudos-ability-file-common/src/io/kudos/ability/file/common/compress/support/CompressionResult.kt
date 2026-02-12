package io.kudos.ability.file.common.compress.support

import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

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

    fun writeTo() {
        if (outputStream != null) {
            try {
                FileOutputStream(outputFilePath).use { out ->
                    outputStream.writeTo(out)
                }
            } catch (ignored: Exception) {
            }
        }
    }
}
