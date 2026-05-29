package io.kudos.ability.log.audit.common.annotation

/**
 * Marks a field on a request-body DTO as **sensitive in the audit log**: the field's value is
 * masked before the request body is written into the audit trail.
 *
 * Masking is applied at the JSON-text level by [io.kudos.ability.log.audit.common.support.AuditLogTool]:
 * every key matching an annotated field name in the parsed request body is replaced with
 * `head1 + "****" + tail3` (whole value masked to `"****"` when length ≤ 4). Class-hierarchy
 * traversal includes inherited fields, so DTOs that extend a common base class still get covered.
 *
 * `@Target(AnnotationTarget.FIELD)` so a Kotlin property declaration like
 * `@LogDesensitize var phone: String? = null` lands the annotation on the backing field — no need
 * for `@field:LogDesensitize` boilerplate at every call site. Reflection in [AuditLogTool] looks at
 * `Class.getDeclaredFields()`, which is where the JVM puts that backing field.
 *
 * Usage:
 * ```kotlin
 * data class CreateAccountRequest(
 *     val username: String,
 *     @LogDesensitize
 *     val phone: String,
 *     @LogDesensitize
 *     val idCard: String,
 * )
 *
 * @WebAudit(...)
 * @PostMapping("/accounts")
 * fun create(@RequestBody req: CreateAccountRequest): Result { ... }
 * ```
 * The audited request body shows `phone` and `idCard` as `1****456` / `5****321`, not the raw values.
 *
 * **Scope**: only the top-level JSON object's keys are masked. Nested objects aren't traversed —
 * if the sensitive field lives inside a nested DTO, annotate the field there and surface it on the
 * top-level request DTO via the same name.
 *
 * Ported from `org.soul.ability.log.audit.common.annotation.LogDesensitize`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class LogDesensitize
