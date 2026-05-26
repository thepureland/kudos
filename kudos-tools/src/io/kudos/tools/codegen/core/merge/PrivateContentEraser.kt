package io.kudos.tools.codegen.core.merge

import io.kudos.base.io.FileKit
import java.io.File

/**
 * Private-content eraser.
 *
 * @author K
 * @since 1.0.0
 */
object PrivateContentEraser {

    fun erase(file: File) {
        val content = FileKit.readFileToString(file)
            .replace(REGION_BEGIN, "")
            .replace(REGION_END, "")
        FileKit.write(file, content)
    }

    private val REGION_BEGIN = Regex("(<!--)?#?//region append \\w+ codes (\\d)(-->)?\\r\\n")
    private val REGION_END = Regex("(<!--)?#?//endregion append \\w+ codes \\d(-->)?")

}