package io.kudos.base.tree

import kotlin.test.Test


/**
 * test for ListToTreeConverter
 *
 * @author K
 * @since 1.0.0
 */
internal class ListToTreeConvertorTest {

    @Test
    fun testConvert() {
        val list = listOf(
            TestTreeNode("10", null, "根结点10"),
            TestTreeNode("11", "10", "10的子结点11"),
            TestTreeNode("12", "10", "10的子结点12"),
            TestTreeNode("20", null, "根结点20"),
            TestTreeNode("21", "20", "20的子结点21")
        )
        val treeList = ListToTreeConverter.convert(list)
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

}