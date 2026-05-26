package io.kudos.ability.cache.interservice.aop

import io.kudos.ability.cache.interservice.common.ClientCacheKey
import io.kudos.ability.cache.interservice.provider.web.CacheClientRequest
import jakarta.servlet.http.HttpServletRequest
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Pointcut
import org.springframework.context.annotation.Lazy
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder

/**
 * Client cache aspect.
 *
 * Implements inter-service caching via AOP, supporting request-based cache-hit detection
 * and conditional data return.
 *
 * Core features:
 * 1. Cache-hit detection: compares the cache-uid in request headers with the uid of the
 *    response result to determine whether the cache is hit.
 * 2. Cache status notification: informs the client of the cache state via HTTP response headers.
 *    - STATUS_USE_CACHE(304): cache hit, the client should use its local cache.
 *    - STATUS_DO_CACHE(200): cache miss, fresh data is returned and the client should cache it.
 * 3. Result UID generation: produces a unique identifier (MD5 hash) from the method return value,
 *    used to validate cache consistency.
 *
 * Workflow:
 * - Intercept Controller methods annotated with @ClientCacheable.
 * - Execute the method to obtain the return value.
 * - Read the cache-uid the client sent in the request header.
 * - Compute the uid of the result (based on class name and JSON-serialized content).
 * - Compare request uid and result uid:
 *   - Equal: return null, set response header to 304, instruct the client to use its cache.
 *   - Different: return the result, set response header to 200, instruct the client to cache the new data.
 *
 * Notes:
 * - May only be used on methods of Controller classes.
 * - Must be used together with CacheClientRequest; plain HTTP requests do not trigger cache logic.
 * - The result object must be JSON-serializable so a uid can be generated.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Aspect
@Lazy(false)
@Order(100) // Cross-service HTTP response-level cache: must wrap outermost so the final result after all inner cache reads/writes is used when computing the UID.
class ClientCacheableAspect(
    private val uidGenerator: ClientCacheUidGenerator = ClientCacheUidGenerator()
) {
    /**
     * Define the pointcut.
     *
     * @author K
     * @since 1.0.0
     */
    @Pointcut("@annotation(io.kudos.ability.cache.interservice.aop.ClientCacheable)")
    private fun cut() {
    }

    /**
     * Around advice: implements the core inter-service caching logic.
     *
     * Workflow:
     * 1. Validate that the target class is a Controller (annotated with RestController or Controller).
     * 2. Invoke the target method and capture the return value.
     * 3. If the result is null, return null directly.
     * 4. Obtain the HTTP request object from RequestContextHolder.
     * 5. Check whether the request is a CacheClientRequest (Feign client request):
     *    - Yes: run the cache-hit detection logic.
     *    - No: return the result directly, with no cache processing.
     *
     * Cache-hit detection logic:
     * 1. Read the cache-uid the client sent in the request header (unique identifier of the client's cache).
     * 2. Compute the uid of the server's result (MD5 hash of class name and JSON-serialized content).
     * 3. Write the result uid into the response header for the client's cache to use.
     * 4. If the request uid is empty, return the result directly with status 200 (needs to cache).
     * 5. Compare request uid and result uid:
     *    - Equal: cache hit, return null and set the response header to 304 (use cache).
     *    - Different: cache miss, return the result and set the response header to 200 (needs to cache).
     *
     * Response headers:
     * - cache-uid: unique identifier of the server's result.
     * - cache-status: cache status; 304 means use cache, 200 means needs to cache.
     *
     * Notes:
     * - Only CacheClientRequest-typed requests trigger the cache logic.
     * - On a cache hit, null is returned and the client should use its local cached data.
     * - The result object must be JSON-serializable so a uid can be generated.
     *
     * @param joinPoint the join point, containing target method info and arguments.
     * @return the target method's return value; null on cache hit.
     */
    @Around("cut()")
    @Throws(Throwable::class)
    fun around(joinPoint: ProceedingJoinPoint): Any? {
        validateClass(joinPoint)
        // Invoke the method and capture the result
        val result = joinPoint.proceed()
        if (result == null) {
            return null
        }
        val requestAttributes = RequestContextHolder.getRequestAttributes() ?: return result
        val request = requestAttributes.resolveReference(RequestAttributes.REFERENCE_REQUEST) as HttpServletRequest
        if (request !is CacheClientRequest) {
            return result
        }
        val response = request.getServletResponse() ?: return result
        val reqUid: String? = request.getHeader(ClientCacheKey.HEADER_KEY_CACHE_UID)
        val resUid: String = uidGenerator.generate(result)
        response.setHeader(ClientCacheKey.HEADER_KEY_CACHE_UID, resUid)
        response.setHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS, ClientCacheKey.STATUS_DO_CACHE)
        if (reqUid.isNullOrBlank()) {
            return result
        }
        if (resUid == reqUid) {
            response.setHeader(ClientCacheKey.HEADER_KEY_CACHE_STATUS, ClientCacheKey.STATUS_USE_CACHE)
            return null
        }
        return result
    }

    /**
     * Validate that the class is a controller.
     *
     * @param joinPoint
     */
    private fun validateClass(joinPoint: ProceedingJoinPoint) {
        val clazz: Class<*> = joinPoint.target.javaClass
        require(clazz.isAnnotationPresent(RestController::class.java) || clazz.isAnnotationPresent(Controller::class.java)) {
            "Class ${clazz.name} must be a Controller to use ClientCacheable!"
        }
    }

}
