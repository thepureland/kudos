package io.kudos.ms.sys.common.i18n.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * 国际化缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class SysI18nCacheEntry (

    /** 主键 */
    override val id: String?,


    /** 语言_地区 */
    val locale: String,

    /** 原子服务编码 */
    val atomicServiceCode: String,

    /** 国际化类型字典代码 */
    val i18nTypeDictCode: String,

    /** 国际化命名空间 */
    val namespace: String,

    /** 国际化key */
    val key: String,

    /** 国际化值 */
    val value: String,

) : IIdEntity<String?>, Serializable {

    companion object {
        private const val serialVersionUID = 6101001001001001011L
    }

}
