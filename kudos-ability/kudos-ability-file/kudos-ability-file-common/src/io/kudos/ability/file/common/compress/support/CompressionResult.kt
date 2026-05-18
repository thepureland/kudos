package io.kudos.ability.file.common.compress.support

import io.kudos.base.logger.LogFactory
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

/**
 * 图像压缩结果。封装内存里的 [outputStream]、目标文件路径、最终 MIME 类型。
 *
 * @author K
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
     * 把内存里的压缩结果落盘到 [outputFilePath]。
     *
     * 历史问题：旧实现 `catch (ignored: Exception) { }` 默默吞掉所有 IO 错误——
     * 压缩失败时调用方完全不知道，文件可能根本没写成功。改为 log error 让事故可见。
     * 仍不向上抛是为了兼容旧调用方（[io.kudos.ability.file.common.compress.CompressionPipeline.compressAndOutputFile]
     * 不声明 IOException），后续可考虑改为抛出。
     */
    fun writeTo() {
        if (outputStream != null) {
            try {
                FileOutputStream(outputFilePath).use { out ->
                    outputStream.writeTo(out)
                }
            } catch (e: Exception) {
                log.error(e, "图像压缩结果写入文件失败：path={0}", outputFilePath)
            }
        }
    }

    companion object {
        private val log = LogFactory.getLog(CompressionResult::class)
    }
}
