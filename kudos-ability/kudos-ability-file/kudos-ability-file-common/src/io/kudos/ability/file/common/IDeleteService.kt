package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DeleteFileModel
import java.io.File

interface IDeleteService {
    /**
     * 删除文件
     * @param model 请求路径
     * @return 是否删除成功
     * @throws ServiceException 文件不存在
     */
    fun delete(model: DeleteFileModel): Boolean

    /**
     * Path是否合法
     * @param model 请求路径
     * @return 是否合法路径
     */
    fun isValid(model: DeleteFileModel): Boolean {
        val relativePath = model.bucketName + File.pathSeparator + model.filePath
        return relativePath.isNotBlank() && !relativePath.contains("..")
    }

}