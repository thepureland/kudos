package io.kudos.ability.file.common.code

import io.kudos.base.enums.ienums.IErrorCodeEnum
import java.text.MessageFormat

/**
 * 文件错误代码
 */
enum class FileErrorCode(
    override val code: String,
    override val trans: String,
    override val printAllStackTrace: Boolean = false
) : IErrorCodeEnum {

    FILE_INVALID_ACCESS_KEY("FS00000000", "无效认证信息"),
    FILE_ACCESS_ERROR("FS00000001", "文件访问错误"),
    FILE_ACCESS_DENY("FS00000002", "文件访问受限"),
    FILE_NO_EXISTS("FS00000003", "文件不存在"),
    FILE_UPLOAD_FAIL("FS00000004", "文件上传失败"),
    FILE_DELETE_FAIL("FS00000005", "文件删除失败");

    fun getMessage(vararg params: Any): String {
        return MessageFormat.format(this.trans, *params)
    }
}
