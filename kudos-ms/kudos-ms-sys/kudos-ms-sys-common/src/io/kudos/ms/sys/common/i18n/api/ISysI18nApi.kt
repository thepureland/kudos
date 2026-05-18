package io.kudos.ms.sys.common.i18n.api

import io.kudos.ms.sys.common.i18n.vo.request.SysI18nFormUpdate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 国际化 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysI18nApi {


    @GetMapping("/api/internal/sys/i18n/getI18nValue")
    fun getI18nValue(
        @RequestParam locale: String,
        @RequestParam i18nTypeDictCode: String,
        @RequestParam namespace: String,
        @RequestParam atomicServiceCode: String,
        @RequestParam key: String
    ): String?

    @GetMapping("/api/internal/sys/i18n/getI18ns")
    fun getI18ns(
        @RequestParam locale: String,
        @RequestParam i18nTypeDictCode: String,
        @RequestParam namespace: String,
        @RequestParam atomicServiceCode: String
    ): Map<String, String>

    @PostMapping("/api/internal/sys/i18n/batchSaveOrUpdate")
    fun batchSaveOrUpdate(@RequestBody i18ns: List<SysI18nFormUpdate>): Int

    @PutMapping("/api/internal/sys/i18n/updateActive")
    fun updateActive(@RequestParam id: String, @RequestParam active: Boolean): Boolean


}
