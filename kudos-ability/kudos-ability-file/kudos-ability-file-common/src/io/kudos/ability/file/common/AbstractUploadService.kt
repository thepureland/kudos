package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult
import java.util.*

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
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val fpLs = ArrayList<String?>()
        if (!model.tenantId.isNullOrBlank()) {
            fpLs.add(model.tenantId)
        }
        if (!model.category.isNullOrBlank()) {
            //优先使用:分类目录
            fpLs.add(model.category)
        } else {
            //默认分类:年/月/日
            fpLs.add(year.toString())
            fpLs.add(month.toString())
            fpLs.add(day.toString())
        }
        //waring: 使用/作为分割符,适合windows cmd + unix like shell + web browsers
        return fpLs.toTypedArray().joinToString("/")
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
