package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import java.io.Serializable

class DeleteFileModel : Serializable {
    /**
     * 自定义目录|存储空间名称
     */
    var bucketName: String? = null

    /**
     * 完整文件路径
     */
    var filePath: String? = null

    /**
     * 认证参数
     */
    var authServerParam: AuthServerParam? = null

    companion object {
        /**
         * 从形如 `/<bucket>/<file/path>` 的完整路径拆出 [bucketName] / [filePath]。
         *
         * **要求 `fullPath` 以 `/` 开头**——这是历史约定（split 后 segments[0] 是空串、
         * segments[1] 是 bucket）。无前导 `/` 时旧实现会把首段当 bucket 后段当 path（错位
         * 一格），这里改成显式 require 拒绝。
         */
        fun from(fullPath: String): DeleteFileModel {
            require(fullPath.isNotBlank()) { "fullPath must not be blank" }
            require(fullPath.startsWith("/")) { "fullPath must start with '/': $fullPath" }
            val segments = fullPath.split('/').dropLastWhile { it.isEmpty() }
            val bucketName = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("fullPath must contain bucket segment: $fullPath")
            val filePath = fullPath.removePrefix("/$bucketName")
            return DeleteFileModel().apply {
                this.bucketName = bucketName
                this.filePath = filePath
            }
        }
    }
}
