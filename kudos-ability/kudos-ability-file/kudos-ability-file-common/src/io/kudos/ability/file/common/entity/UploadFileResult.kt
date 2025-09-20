package io.kudos.ability.file.common.entity

import java.io.Serial
import java.io.Serializable

/**
 * 上传文件结果
 * 如：文件存放在硬盘位置为（/var/file/upload/console/-99/2022/11/09/123456789.jpg）
 * 其中：/var/file/upload/ 为规划的文件上传路径，之后的为应用上传后存放的路径。
 * 那么返回的内容：filepath=console/-99/boss/2022/11/09/123456789.jpg;pathPrefix=/var/file/upload/
 * 完整路径为：pathPrefix+filePath;
 * 基本上本地硬盘，则无需关注filePath。
 */
class UploadFileResult : Serializable {
    /**
     * 文件相对路径：文件存放相对位置
     */
    var filePath: String? = null

    /**
     * 文件路径前缀：可能是硬盘，可能是url地址
     */
    var pathPrefix: String? = null

    companion object {
        @Serial
        private val serialVersionUID = -8544059935004940300L
    }
}
