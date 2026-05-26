package io.kudos.ms.sys.common.i18n.vo.request

/**
 * Request VO for batch i18n information.
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nBatchPayload(

    /** Language region */
    val locale: String = "",

    /** Map<i18n type dictionary code, Collection<namespace>> */
    val namespacesByI18nTypeDictCode: Map<String, Collection<String>> = emptyMap(),

    /** Set of atomic service codes */
    val atomicServiceCodes: Set<String> = emptySet()

)