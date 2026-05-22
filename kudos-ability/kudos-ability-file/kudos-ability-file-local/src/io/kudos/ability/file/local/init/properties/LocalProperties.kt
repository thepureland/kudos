package io.kudos.ability.file.local.init.properties

/**
 * 本地文件存储配置；对应 `kudos.ability.file.local.*`。
 *
 * @property basePath 文件存储根目录的绝对路径。所有 bucket / file 都落在此目录下。
 *   留空 / 配置错误时上传 / 下载 / 删除会因找不到目录失败。生产部署强烈建议设到挂载点。
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LocalProperties {
    var basePath: String? = null
}
