package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.result.IdJsonResult

/**
 * 字典树节点
 *
 * @author K
 * @since 1.0.0
 */
class SysDictTreeNode: IdJsonResult<String>() {

    /** 字典类型或字典项编码 */
    var code: String? = null

    override var id: String = ""

}