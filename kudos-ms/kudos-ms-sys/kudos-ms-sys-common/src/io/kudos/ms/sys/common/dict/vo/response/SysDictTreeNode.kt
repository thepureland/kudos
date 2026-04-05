package io.kudos.ms.sys.common.dict.vo.response
import io.kudos.base.model.contract.result.IdJsonResult

/**
 * 字典树结点响应VO
 *
 * @author K
 * @since 1.0.0
 */
class SysDictTreeNode: IdJsonResult<String>() {

    /** 字典类型或字典项编码 */
    var code: String? = null

    override var id: String = ""

}