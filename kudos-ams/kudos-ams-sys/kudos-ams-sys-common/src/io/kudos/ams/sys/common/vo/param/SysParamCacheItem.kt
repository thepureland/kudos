package io.kudos.ams.sys.common.vo.param

import java.io.Serializable


/**
 * 参数缓存项
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysParamCacheItem : Serializable {
//endregion your codes 1

    //region your codes 2

    companion object {
        private const val serialVersionUID = 6322052105466594633L
    }

    /** 模块编码 */
    var moduleCode: String? = null

    /** 参数名称 */
    var paramName: String? = null

    /** 参数值，或其国际化key */
    var paramValue: String? = null

    /** 默认参数值，或其国际化key */
    var defaultValue: String? = null

    /** 序号 */
    var seqNo: Int? = null

    /** 是否内置 */
    var builtIn: Boolean? = null

    /** 备注 */
    var remark: String? = null

    //endregion your codes 2


}