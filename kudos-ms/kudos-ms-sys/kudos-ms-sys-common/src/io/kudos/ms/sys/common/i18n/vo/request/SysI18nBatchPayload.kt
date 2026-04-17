package io.kudos.ms.sys.common.i18n.vo.request

/**
 * 批量i18n信息的请求Vo
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nBatchPayload(

    /** 语言地区 */
    val locale: String = "",

    /** Map<国际化类型字典代码，Collection<命名空间>> */
    val namespacesByI18nTypeDictCode: Map<String, Collection<String>> = emptyMap(),

    /** 原子服务编码集合 */
    val atomicServiceCodes: Set<String> = emptySet()

)