package io.kudos.ms.sys.common.dict.vo.response

import io.kudos.base.model.contract.result.IdJsonResult

/**
 * Response VO for a dict tree node.
 *
 * @author K
 * @since 1.0.0
 */
class SysDictTreeNode: IdJsonResult<String>() {

    /** Dict type or dict item code */
    var code: String? = null

    override var id: String = ""

}