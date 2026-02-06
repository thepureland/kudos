package io.kudos.ability.file.common.enums

/**
 * https://tool.oschina.net/commons
 * 参考上述网站对应关系，后续有额外增加要求需要补充
 */
enum class UploadContentTypeEnum(var fileSuffix: String, var contentType: String) {
    DEFAULT("default", "application/octet-stream"),  //
    PNG("png", "image/png"),  //
    JPG("jpg", "image/jpeg"),  //
    JPEG("jpeg", "image/jpeg"),  //
    GIF("gif", "image/gif"),  //
    DOC("doc", "application/msword"),  //
    DOCX("docx", "application/msword"),  //
    XLS("xls", "application/x-xls"),  //
    XLSX("xlsx", "application/x-xls"),  //
    BMP("pdf", "application/x-bmp"),  //
    PDF("pdf", "application/pdf"),  //
    WEBP("webp", "image/webp"); //

    companion object {
        fun enumOf(fileSuffix: String): UploadContentTypeEnum {
            if (fileSuffix.isBlank()) {
                return DEFAULT
            }
            for (value in entries) {
                if (value.fileSuffix.equals(fileSuffix, ignoreCase = true)) {
                    return value
                }
            }
            return DEFAULT
        }
    }
}
