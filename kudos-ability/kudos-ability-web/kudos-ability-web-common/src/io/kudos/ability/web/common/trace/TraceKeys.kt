package io.kudos.ability.web.common.trace

import io.kudos.base.lang.string.RandomStringKit

/**
 * Trace-key policy shared by every web runtime.
 *
 * The rules below are security-relevant rather than cosmetic, and they were previously implemented in the
 * Spring MVC runtime only — which is exactly how two runtimes of the same framework end up disagreeing about
 * what a caller is allowed to send. This module's own extraction rule ("lift it once it appears twice") applies:
 * the second implementation is what makes this shared.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object TraceKeys {

    /**
     * The public trace header, used by browsers and any external caller, and the name a response echoes the
     * resolved trace key back on.
     *
     * Deliberately not in `Consts.RequestHeader`: that object is the namespace for *internal* cross-process
     * headers, whose `_` prefix convention is enforced by its own test. `_UUID` lives there and stays there —
     * it is what Feign puts on service-to-service calls and what the request-signature verifier folds into its
     * signature. This one is the outward-facing name, so it belongs to the web layer.
     */
    const val TRACE_ID_HEADER: String = "X-Trace-Id"

    /** Default upper bound on an accepted caller-supplied trace key. */
    const val DEFAULT_MAX_LENGTH: Int = 128

    /**
     * Whether a caller-supplied trace key may be propagated as-is.
     *
     * A trace key is interpolated into log lines and echoed into a response header, so an unvalidated one lets
     * a caller embed CR/LF and thereby forge log entries or split the response headers. An unbounded length is
     * additionally a cheap lever against any log or metrics backend that indexes by trace id.
     *
     * @param value the raw header value, or null when the header is absent
     * @param maxLength upper bound on the accepted length
     * @return true when the value is safe to propagate
     * @author K
     * @since 1.0.0
     */
    fun isAcceptable(value: String?, maxLength: Int = DEFAULT_MAX_LENGTH): Boolean =
        value != null &&
            value.isNotBlank() &&
            value.length <= maxLength &&
            value.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }

    /**
     * Resolve the trace key for a request, falling back to a generated one.
     *
     * Headers are consulted in the given order and the first *acceptable* value wins. A header carrying an
     * unacceptable value is skipped rather than fatal, so one bad header does not discard a good value in the
     * next — which matters as soon as more than one header is in play, since a caller controls all of them.
     *
     * @param headerNames headers to consult, in precedence order; blank names are ignored
     * @param maxLength upper bound on an accepted value
     * @param lookup reads a request header by name, returning null when absent
     * @return the first acceptable caller-supplied trace key, otherwise a fresh UUID
     * @author K
     * @since 1.0.0
     */
    fun resolve(
        headerNames: List<String>,
        maxLength: Int = DEFAULT_MAX_LENGTH,
        lookup: (String) -> String?
    ): String {
        headerNames.forEach { name ->
            if (name.isBlank()) return@forEach
            val supplied = lookup(name)
            if (isAcceptable(supplied, maxLength)) return supplied!!
        }
        return RandomStringKit.uuid()
    }

}
