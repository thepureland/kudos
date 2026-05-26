package io.kudos.ms.sys.common.i18n.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * I18n cache entry.
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nCacheEntry (

    /** Primary key */
    override val id: String?,


    /** Language_region */
    val locale: String,

    /** Atomic service code */
    val atomicServiceCode: String,

    /** I18n type dict code */
    val i18nTypeDictCode: String,

    /** I18n namespace */
    val namespace: String,

    /** I18n key */
    val key: String,

    /** I18n value */
    val value: String,

) : IIdEntity<String?>, Serializable {

    companion object {
        private const val serialVersionUID = 6101001001001001011L
    }

}
