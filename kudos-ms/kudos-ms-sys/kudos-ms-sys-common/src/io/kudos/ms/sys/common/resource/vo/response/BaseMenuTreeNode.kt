package io.kudos.ms.sys.common.resource.vo.response
import io.kudos.base.model.contract.result.IJsonResult
import io.kudos.base.tree.ITreeNode
import java.beans.Transient


/**
 * 基础系统菜单树结点响应VO
 *
 * @author K
 * @since 1.0.0
 */
open class BaseMenuTreeNode: IJsonResult, ITreeNode<String>, Comparable<BaseMenuTreeNode> {

    /** 名称，或其国际化key */
    var title: String? = null

    /** id */
    var id: String? = null

    /** 父id */
    @get:Transient
    var parentId: String? = null

    /** 在同父节点下的排序号 */
    @get:Transient
    var seqNo: Int? = null


    /** 孩子结点 */
//    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) //TODO
    var children = mutableListOf<ITreeNode<String>>()

    override fun _getId(): String = requireNotNull(id) { "BaseMenuTreeNode.id 不能为空" }

    override fun _getParentId(): String? = parentId

    override fun _getChildren(): MutableList<ITreeNode<String>> = children

    override fun compareTo(other: BaseMenuTreeNode): Int {
        val a = seqNo ?: return 0
        val b = other.seqNo ?: return 0
        return a.compareTo(b)
    }

}