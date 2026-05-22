package io.kudos.ability.file.common.compress.utils

import io.kudos.base.io.FilenameKit
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * 图像压缩相关的小工具集合。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CompressUtil {

    /** 当前框架认作"可压缩图片"的后缀白名单。新增类型时一并加这里 + [io.kudos.ability.file.common.compress.support.ImageCompressorFactory]。 */
    private val EXTENSIONS = setOf("jpg", "jpeg", "png")

    /** 文件名是否对应"可压缩图片"（按后缀判定，不读文件头）。 */
    fun isPic(fileName: String): Boolean = validExtension(FilenameKit.getExtension(fileName))

    /** 后缀是否在 [EXTENSIONS] 白名单内；大小写转换走 [Locale.ROOT] 规避 Turkish locale 偏差。 */
    fun validExtension(extension: String): Boolean {
        if (extension.isBlank()) return false
        return EXTENSIONS.contains(extension.lowercase(Locale.ROOT))
    }

    /** JDK [Files.probeContentType] 的简单转发；返回 null 表示无法识别。 */
    @Throws(IOException::class)
    fun mimeType(fileName: String): String? = Files.probeContentType(Path.of(fileName))

}
