package io.kudos.ms.sys.common.vo.dict

import io.kudos.base.support.result.IdJsonResult


class SysDictTreeNode: IdJsonResult<String>() {

    /** 字典类型或字典项编码 */
    var code: String? = null

}