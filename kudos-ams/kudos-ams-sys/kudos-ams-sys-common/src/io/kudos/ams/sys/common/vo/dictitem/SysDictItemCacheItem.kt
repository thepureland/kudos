package io.kudos.ams.sys.common.vo.dictitem

import java.io.Serializable


/**
 * 字典项缓存项
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
open class SysDictItemCacheItem : Serializable {
//endregion your codes 1

    //region your codes 2

    companion object {
        private const val serialVersionUID = 1931201536817154812L
    }

    /** 字典项编号 */
    var itemCode: String? = null

    /** 字典项名称，或其国际化key */
    var itemName: String? = null

    /** 父项ID */
    var parentId: String? = null

    /** 该字典编号在同父节点下的排序号 */
    var seqNo: Int? = null

    //endregion your codes 2


}