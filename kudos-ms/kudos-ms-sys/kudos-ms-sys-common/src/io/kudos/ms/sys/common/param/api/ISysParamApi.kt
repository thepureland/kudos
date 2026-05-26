package io.kudos.ms.sys.common.param.api

import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for parameters.
 *
 * @author K
 * @since 1.0.0
 */
interface ISysParamApi {


    /**
     * Get the corresponding parameter by parameter name and atomic service code.
     *
     * @param paramName Parameter name
     * @param atomicServiceCode Atomic service code, defaults to "default".
     * @return Parameter cache object. Returns null if not found.
     * @author K
     * @since 1.0.0
     */
    @GetMapping("/api/internal/sys/param/getParam")
    fun getParam(
        @RequestParam paramName: String,
        @RequestParam(required = false, defaultValue = "default") atomicServiceCode: String = "default"
    ): SysParamCacheEntry?


}
