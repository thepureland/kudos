package io.kudos.ms.sys.common.resource.vo.response

import io.kudos.base.model.contract.result.IJsonResult
import io.kudos.base.tree.ITreeNode
import java.beans.Transient


/**
 * Base system menu tree node response VO
 *
 * @author K
 * @since 1.0.0
 */
open class BaseMenuTreeNode: IJsonResult, ITreeNode<String>, Comparable<BaseMenuTreeNode> {

    /** Name, or its i18n key */
    var title: String? = null

    /** id */
    var id: String? = null

    /** Parent id */
    @get:Transient
    var parentId: String? = null

    /** Order number among siblings under the same parent */
    @get:Transient
    var seqNo: Int? = null


    /** Child nodes */
//    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) //TODO
    var children = mutableListOf<ITreeNode<String>>()

    override fun _getId(): String = requireNotNull(id) { "BaseMenuTreeNode.id must not be null" }

    override fun _getParentId(): String? = parentId

    override fun _getChildren(): MutableList<ITreeNode<String>> = children

    override fun compareTo(other: BaseMenuTreeNode): Int {
        val a = seqNo ?: return 0
        val b = other.seqNo ?: return 0
        return a.compareTo(b)
    }

}