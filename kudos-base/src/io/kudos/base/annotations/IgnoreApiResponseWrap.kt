package io.kudos.base.annotations

/**
 * 标记该 controller / 方法**跳过**全局 `ApiResponse` 统一包装。
 *
 * 用于流式响应（文件下载、SSE）或第三方协议（OAuth callback、Webhook）等场景——
 * 这些响应体不能被改写成 `{code, message, data}` 结构，否则破坏对端解析。
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IgnoreApiResponseWrap
