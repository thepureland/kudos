package io.kudos.base.tree

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for TreeKit
 *
 * @author K
 * @since 1.0.0
 */
internal class TreeKitTest {

    @Test
    fun convertListToTree() {
        val list = listOf(
            TestTreeNode("10", null, "根结点10"),
            TestTreeNode("11", "10", "10的子结点11"),
            TestTreeNode("12", "10", "10的子结点12"),
            TestTreeNode("20", null, "根结点20"),
            TestTreeNode("21", "20", "20的子结点21")
        )
        val treeList = TreeKit.convertListToTree(list)
        var result = treeList.size == 2
        val treeNode10 = treeList[0]
        result = result && "10" == treeNode10.id
        result = result && treeNode10._getChildren().size == 2
        val treeNode11 = treeNode10._getChildren()[0]
        result = result && "11" == treeNode11._getId()
        val treeNode12 = treeNode10._getChildren()[1]
        result = result && "12" == treeNode12._getId()
        val treeNode20 = treeList[1]
        result = result && "20" == treeNode20._getId()
        result = result && treeNode20._getChildren().size == 1
        val treeNode21 = treeNode20._getChildren()[0]
        result = result && "21" == treeNode21._getId()
        assert(result)
    }

    @Test
    fun depthTraverse() {
        val rootNode00 = TestTreeNode("00", null, "根结点10")
        val node11 = TestTreeNode("11", "00", "00的子结点11")
        rootNode00.children.add(node11)
        val node21 = TestTreeNode("21", "11", "11的子结点21")
        node11.children.add(node21)
        val node12 = TestTreeNode("12", "00", "00的子结点12")
        rootNode00.children.add(node12)

        val sb = StringBuilder()
        TreeKit.depthTraverse(rootNode00) {
            sb.append(it._getId()).append(",")
        }
        assertEquals("00,11,21,12,", sb.toString())
    }

    @Test
    fun breadthTraverse() {
        val rootNode00 = TestTreeNode("00", null, "根结点10")
        val node11 = TestTreeNode("11", "00", "00的子结点11")
        rootNode00.children.add(node11)
        val node21 = TestTreeNode("21", "11", "11的子结点21")
        node11.children.add(node21)
        val node12 = TestTreeNode("12", "00", "00的子结点12")
        rootNode00.children.add(node12)
        val node13 = TestTreeNode("13", "00", "00的子结点13")
        rootNode00.children.add(node13)
        val node22 = TestTreeNode("22", "13", "13的子结点22")
        node13.children.add(node22)
        val sb = StringBuilder()
        TreeKit.breadthTraverse(rootNode00) {
            sb.append(it._getId()).append(",")
        }
        assertEquals("00,11,12,13,21,22,", sb.toString())
    }

}