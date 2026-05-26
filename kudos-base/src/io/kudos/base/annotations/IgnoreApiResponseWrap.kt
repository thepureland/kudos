package io.kudos.base.annotations

/**
 * Marks the controller / method to **skip** the global `ApiResponse` unified wrapping.
 *
 * Used for streaming responses (file downloads, SSE) or third-party protocols (OAuth callback, Webhook), etc. -
 * these response bodies cannot be rewritten into the `{code, message, data}` structure, otherwise it breaks the peer's parsing.
 *
 * @author K
 * @since 1.0.0
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IgnoreApiResponseWrap
