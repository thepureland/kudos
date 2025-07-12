package io.kudos.ams.sys.common.vo.cache

import java.io.Serializable


/**
 * 缓存缓存项
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysCacheCacheItem : Serializable {
//endregion your codes 1

    //region your codes 2

    companion object {
        private const val serialVersionUID = 1877120481369335391L
    }

    /** 名称 */
    var name: String? = null

    /** 子系统代码 */
    var subSysDictCode: String? = null

    /** 缓存策略代码 */
    var strategyDictCode: String? = null

    /** 是否启动时写缓存 */
    var writeOnBoot: Boolean? = null

    /** 是否及时回写缓存 */
    var writeInTime: Boolean? = null

    /** 缓存生存时间(秒) */
    var ttl: Int? = null

    /** 备注，或其国际化key */
    var remark: String? = null

    /** 是否启用 */
    var active: Boolean? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    //endregion your codes 2


}