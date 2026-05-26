package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult
import java.time.LocalDate

/**
 * Abstract base class for file upload services.
 *
 * Extracts the common flow of "allocate directory -> save file -> assemble result"; subclasses
 * (Local / Minio / OSS) only need to implement [saveFile] and [pathPrefix]. [dispatchFileDir] provides a
 * default directory strategy based on "tenant/category or year/month/day"; subclasses can override to customize.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AbstractUploadService : IUploadService {

    /**
     * Template method: allocate directory -> save file -> assemble [UploadFileResult].
     * Subclasses do not need to override this method; they only need to provide [saveFile] and [pathPrefix].
     *
     * @param model upload request model
     * @return result containing the relative path and path prefix
     * @author K
     * @since 1.0.0
     */
    override fun fileUpload(model: UploadFileModel<*>): UploadFileResult {
        val result = UploadFileResult()
        // allocate file name
        val fileDir = dispatchFileDir(model)
        // save file
        val filePath = saveFile(model, fileDir)
        // 4. set return result
        result.filePath = filePath
        result.pathPrefix = pathPrefix()
        return result
    }

    /**
     * Default directory allocation strategy:
     * - If `tenantId` is specified, use it as the top-level directory for multi-tenant isolation;
     * - If `category` is specified, use it as the next-level directory (business category);
     * - Otherwise bucket by "year/month/day" daily, to avoid an explosion of files in a single directory.
     *
     * `/` is used as the separator because it is compatible with Windows cmd / unix shell / browser URL.
     *
     * @param model upload request model
     * @return relative directory string
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
        // warning: use / as separator, suitable for windows cmd + unix-like shell + web browsers
        return fpLs.joinToString("/")
    }

    /**
     * Save file.
     *
     * @param model   m
     * @param fileDir relative directory of the file
     * @return filePath relative path of the file
     */
    protected abstract fun saveFile(model: UploadFileModel<*>, fileDir: String): String?

    abstract override fun pathPrefix(): String

}
