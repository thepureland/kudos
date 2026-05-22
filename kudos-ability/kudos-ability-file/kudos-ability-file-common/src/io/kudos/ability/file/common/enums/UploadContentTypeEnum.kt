package io.kudos.ability.file.common.enums

/**
 * 文件后缀 ↔ MIME 类型的映射枚举。
 *
 * 参考：https://tool.oschina.net/commons 。需要新文件类型时按字面追加枚举值即可。
 *
 * 历史 bug（已修）：旧实现里 `BMP("pdf", "application/x-bmp")` 后缀错写成了 "pdf"，
 * 导致：
 *  - `enumOf("bmp")` 返回 DEFAULT（找不到匹配）
 *  - `enumOf("pdf")` 返回 BMP（在 PDF 之前匹配到错误的 "pdf" 后缀）→ PDF 文件被
 *    打上 `application/x-bmp` 的 contentType
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
         * 按文件后缀（大小写不敏感）查询枚举；空串或未匹配返回 [DEFAULT]。
         */
        fun enumOf(fileSuffix: String): UploadContentTypeEnum {
            if (fileSuffix.isBlank()) {
                return DEFAULT
            }
            return entries.firstOrNull { it.fileSuffix.equals(fileSuffix, ignoreCase = true) } ?: DEFAULT
        }
    }
}
