package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult
import java.time.LocalDate

abstract class AbstractUploadService : IUploadService {
    
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
     * 分配文件路径
     *
     * @param model
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
