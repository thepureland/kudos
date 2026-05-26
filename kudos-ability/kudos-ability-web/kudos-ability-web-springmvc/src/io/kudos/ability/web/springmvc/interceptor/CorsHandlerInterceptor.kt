package io.kudos.ability.web.springmvc.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.web.servlet.HandlerInterceptor


/**
 * Cross-Origin Resource Sharing handler interceptor.
 *
 * A Spring MVC interceptor that injects CORS-related response headers into the response.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class CorsHandlerInterceptor : HandlerInterceptor {

    /**
     * Invoked before the controller method executes.
     * @return true to continue subsequent processing (including invoking the controller); false to stop here (typically used to short-circuit OPTIONS preflight).
     */
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {

        // Access-Control-Allow-Origin: tells the browser "which origin is allowed to access".
        // Here we simply reflect the request's Origin header as-is.
        // Note: if the request has no Origin (same-origin or non-browser), this may be null.
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"))

        // Access-Control-Allow-Methods: allowed cross-origin methods.
        // Set to "*" here, meaning all methods are allowed.
        // The spec recommends listing specific methods (e.g. GET,POST,PUT,DELETE,OPTIONS).
        response.setHeader("Access-Control-Allow-Methods", "*")

        // Access-Control-Allow-Credentials: whether credentials such as Cookie/Authorization are allowed.
        // Setting true means allowed.
        // **Spec note**: when allow-credentials = true, Allow-Origin **cannot be "*"**, it must be a specific domain.
        response.setHeader("Access-Control-Allow-Credentials", "true")

        // Access-Control-Allow-Headers: the list of custom request headers the client wishes to send in a preflight request.
        // A few common headers are listed manually here; additional custom headers sent by the client may be blocked.
        // "*" is also acceptable (supported by newer spec/browsers), or reflect Access-Control-Request-Headers.
        response.setHeader(
            "Access-Control-Allow-Headers",
            "Authorization,Origin, X-Requested-With, Content-Type, Accept,Access-Token"
        )

        // Return true: continue executing subsequent interceptors/controllers.
        // To short-circuit OPTIONS preflight with HTTP 200, return false here when request.method == "OPTIONS".
        return true
    }
}