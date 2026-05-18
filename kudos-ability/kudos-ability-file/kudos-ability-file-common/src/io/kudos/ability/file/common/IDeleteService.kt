package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DeleteFileModel
import java.io.File

/**
 * 文件删除服务 SPI。具体存储后端（本地磁盘 / MinIO / OSS）各自实现。
 *
 * @author K
 * @since 1.0.0
 */
interface IDeleteService {
    /**
     * 删除文件
     * @param model 请求路径
     * @return 是否删除成功
     * @throws ServiceException 文件不存在
     */
    fun delete(model: DeleteFileModel): Boolean

    /**
     * Path 是否合法——目前仅做粗粒度的 `..` 路径穿越防护。
     *
     * 历史 bug：旧实现拼接时用了 [File.pathSeparator]（环境变量 PATH 的分隔符，
     * Unix `:` / Windows `;`），与"文件路径片段间的分隔"（[File.separator]：Unix `/` /
     * Windows `\`）是两个不同的概念，会让拼接结果错误（如 `bucket:filePath` 而非
     * `bucket/filePath`），进而让 `..` 检查检的不是真实即将被使用的路径形态。已修。
     *
     * 注意：本检查**远不足以替代真正的路径白名单 / 规范化**。生产部署应当在每个具体
     * 后端的 `delete` 中再做 `Path.normalize() + startsWith(rootDir)` 等强校验。
     *
     * @param model 请求路径
     * @return 是否合法路径
     */
    fun isValid(model: DeleteFileModel): Boolean {
        val relativePath = (model.bucketName ?: "") + File.separator + (model.filePath ?: "")
        return relativePath.isNotBlank() && !relativePath.contains("..")
    }

}
