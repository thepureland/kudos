package io.kudos.ability.file.common.auth

/**
 * 文件存储后端认证参数的标记接口。
 *
 * 当前实现：
 *  - [AccessKeyServerParam]：AK / SK 双串（MinIO / S3 / 阿里云 OSS 等）
 *  - [AccessTokenServerParam]：单 token / Bearer header
 *
 * 设计成空接口而非 sealed class，是为了让下游 storage 子模块可以自定义新认证形式
 * （如带 region 的 STS token、Azure SAS URL 等）。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthServerParam
