package io.kudos.ability.file.common.compress.support

import java.io.Serializable

/**
 * 玩家端传入的压缩配置信息
 */
class CompressionConfig : Serializable {

    /**
     * 是否開啓壓縮
     */
    var enabled: Boolean = false

    /**
     * 是否轉換為webp格式
     */
    var webp: Boolean = false

    /**
     * 非webp模式下生效
     */
    var width: Int = 0

    /**
     * 非webp模式下生效
     */
    var height: Int = 0

    /**
     * 非webp模式下生效
     */
    var quality: Float = 1f
        set(quality) {
            require(quality in 0.0f..1.0f) { "Quality must be between 0.0 and 1.0" }
            field = quality
        }
}
