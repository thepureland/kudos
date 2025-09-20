package io.kudos.ability.file.common.compress.utils

import io.kudos.base.io.FilenameKit
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

object CompressUtil {

    private val extensions = mutableListOf<String?>("jpg", "jpeg", "png")

    fun isPic(fileName: String): Boolean {
        val extension: String = FilenameKit.getExtension(fileName)
        return validExtension(extension)
    }

    fun validExtension(extension: String): Boolean {
        if (extension.isBlank()) {
            return false
        }
        return extensions.contains(extension.lowercase(Locale.getDefault()))
    }

    @Throws(IOException::class)
    fun mimeType(fileName: String): String? {
        return Files.probeContentType(Path.of(fileName))
    }

}
