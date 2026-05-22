package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult
import java.time.LocalDate

/**
 * 文件上传服务抽象基类。
 *
 * 提炼"分配目录 → 保存文件 → 拼装结果"的通用流程，子类（Local / Minio / OSS）只需实现 [saveFile] 与 [pathPrefix]。
 * [dispatchFileDir] 给出默认按"租户/分类 或 年/月/日"的目录策略，子类可重写定制。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AbstractUploadService : IUploadService {

    /**
     * 模板方法：分配目录 → 保存文件 → 拼装 [UploadFileResult]。
     * 子类不需要重写本方法，只需补齐 [saveFile] 与 [pathPrefix]。
     *
     * @param model 上传请求模型
     * @return 包含相对路径与路径前缀的结果
     * @author K
     * @since 1.0.0
     */
    override fun fileUpload(model: UploadFileModel<*>): UploadFileResult {
        val result = UploadFileResult()
        // 分配文件名
        val fileDir = dispatchFileDir(model)
        // 保存文件
        val filePath = saveFile(model, fileDir)
        //4 设置返回结果
        result.filePath = filePath
        result.pathPrefix = pathPrefix()
        return result
    }

    /**
     * 默认目录分配策略：
     * - 若指定 `tenantId`，作为最前一级目录用于多租户隔离；
     * - 若指定 `category`，作为下一级目录（业务分类）；
     * - 否则按"年/月/日"按日分桶，避免单目录文件爆炸。
     *
     * 用 `/` 作分隔符是因为兼容 Windows cmd / unix shell / 浏览器 URL 这三种场景。
     *
     * @param model 上传请求模型
     * @return 相对目录字符串
     * @author K
     * @since 1.0.0
     */
    protected open fun dispatchFileDir(model: UploadFileModel<*>): String {
        val today = LocalDate.now()
        val fpLs = mutableListOf<String>()
        model.tenantId?.takeIf { it.isNotBlank() }?.let { fpLs.add(it) }
        val category = model.category?.takeIf { it.isNotBlank() }
        if (category != null) {
            fpLs.add(category)
        } else {
            fpLs.add(today.year.toString())
            fpLs.add(today.monthValue.toString())
            fpLs.add(today.dayOfMonth.toString())
        }
        //waring: 使用/作为分割符,适合windows cmd + unix like shell + web browsers
        return fpLs.joinToString("/")
    }

    /**
     * 保存文件
     *
     * @param model   m
     * @param fileDir 文件相对目录
     * @return filePath 文件相对路径
     */
    protected abstract fun saveFile(model: UploadFileModel<*>, fileDir: String): String?

    abstract override fun pathPrefix(): String

}
