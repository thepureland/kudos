package io.kudos.ability.file.common.enums

/**
 * Mapping enum of file suffix to MIME type.
 *
 * Reference: https://tool.oschina.net/commons . Simply append new enum values
 * verbatim when new file types are needed.
 *
 * Historical bug (fixed): in the old implementation `BMP("pdf", "application/x-bmp")`
 * had its suffix erroneously written as "pdf", which caused:
 *  - `enumOf("bmp")` to return DEFAULT (no match found)
 *  - `enumOf("pdf")` to return BMP (matched the wrong "pdf" suffix before PDF)
 *    -> PDF files were tagged with the `application/x-bmp` contentType
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
enum class UploadContentTypeEnum(var fileSuffix: String, var contentType: String) {
    DEFAULT("default", "application/octet-stream"),
    PNG("png", "image/png"),
    JPG("jpg", "image/jpeg"),
    JPEG("jpeg", "image/jpeg"),
    GIF("gif", "image/gif"),
    DOC("doc", "application/msword"),
    DOCX("docx", "application/msword"),
    XLS("xls", "application/x-xls"),
    XLSX("xlsx", "application/x-xls"),
    BMP("bmp", "image/bmp"),
    PDF("pdf", "application/pdf"),
    WEBP("webp", "image/webp");

    companion object {
        /**
         * Looks up the enum by file suffix (case-insensitive); returns [DEFAULT] when empty or unmatched.
         */
        fun enumOf(fileSuffix: String): UploadContentTypeEnum {
            if (fileSuffix.isBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.fileSuffix.equals(fileSuffix, ignoreCase = true) } ?: DEFAULT
        }
    }
}
