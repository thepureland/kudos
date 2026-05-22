package io.kudos.ability.file.common.auth

/**
 * AccessToken（HTTP Header 注入式）鉴权参数：把 `headerValue` 直接放进每次上传请求的 Authorization header。
 *
 * 用于 [io.kudos.ability.file.minio.MinioUploadService] 等场景的"动态鉴权"——
 * 由调用方按租户/会话注入 token，区别于配置文件预置的静态 AK/SK 模式。
 *
 * @property headerValue Authorization header 的完整值（含 `Bearer ` 前缀等格式由调用方负责）
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class AccessTokenServerParam : AuthServerParam {
    var headerValue: String? = null
}
