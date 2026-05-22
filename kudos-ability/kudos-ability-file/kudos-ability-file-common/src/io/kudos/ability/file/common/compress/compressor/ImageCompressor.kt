package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.InputStream

/**
 * 图片压缩协议。
 *
 * 由 [io.kudos.ability.file.common.compress.support.ImageCompressorFactory] 按 [CompressionConfig]
 * 挑选合适的实现（Jpg / Png / WebP）。新增压缩格式时实现本接口并在工厂里登记即可。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface ImageCompressor {

    /**
     * 判断当前压缩器是否处理这次任务。
     * 例如 [WebPCompressor] 只在 `config.webp = true` 时返回 true，让上层在普通图片场景跳过 WebP。
     *
     * @param config 压缩配置
     * @return true 表示由本压缩器处理
     */
    fun support(config: CompressionConfig): Boolean

    /**
     * 执行压缩。
     *
     * @param inputStream 源图片输入流（调用方负责关闭）
     * @param destination 期望的目标路径（具体实现可能改名，如 WebP 追加 `.webp` 后缀）
     * @param config 压缩配置
     * @return 含压缩字节流、最终路径、Content-Type 的结果
     */
    fun compress(inputStream: InputStream, destination: String, config: CompressionConfig): CompressionResult

}
