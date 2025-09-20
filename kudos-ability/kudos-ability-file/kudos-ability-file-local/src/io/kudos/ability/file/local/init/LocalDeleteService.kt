package io.kudos.ability.file.local.init

import io.kudos.ability.file.common.IDeleteService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File


class LocalDeleteService : IDeleteService {
    
    @Autowired
    private lateinit var properties: LocalProperties

    public override fun delete(model: DeleteFileModel): Boolean {
        if (!this.isValid(model)) {
            return false
        }

        val fullPath = (properties.basePath
                + File.separator + model.bucketName
                + File.separator + model.filePath)
        val file = File(fullPath)
        if (file.isDirectory()) {
            LOG.warn("can't delete a folder: $fullPath")
            return false
        }
        if (!file.exists()) {
            LOG.warn("file path is not exists: $fullPath")
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
        LOG.warn("file delete")
        return file.delete()
    }

    private val LOG= LogFactory.getLog(this)

}
