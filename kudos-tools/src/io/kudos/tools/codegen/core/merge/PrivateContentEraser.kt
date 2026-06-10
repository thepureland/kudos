package io.kudos.tools.codegen.core.merge

import io.kudos.base.io.FileKit
import java.io.File

/**
 * Private-content eraser: strips the `//region append ... codes N` marker lines from a freshly
 * generated file. The markers only exist to assist [CodeMerger] on re-generation and must not
 * appear in a first-time generation result.
 *
 * @author K
 * @since 1.0.0
 */
object PrivateContentEraser {

    /**
     * Removes all append-region begin/end markers from [file] in place.
     *
     * @param file the freshly generated file
     * @author K
     * @since 1.0.0
     */
    fun erase(file: File) {
        val content = FileKit.readFileToString(file)
            .replace(REGION_BEGIN, "")
            .replace(REGION_END, "")
        FileKit.write(file, content)
    }

    /** Begin marker, including its trailing line break (LF and CRLF are both accepted) */
    private val REGION_BEGIN = Regex("(<!--)?#?//region append \\w+ codes (\\d)(-->)?\\r?\\n")
    /** End marker (the line break before it belongs to the region body and is kept) */
    private val REGION_END = Regex("(<!--)?#?//endregion append \\w+ codes \\d(-->)?")

}