package io.kudos.ability.ui.javafx.controls.wizard

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.scene.Group
import javafx.scene.Node
import javafx.scene.Parent
import javafx.scene.control.Button
import javafx.scene.control.Skin
import javafx.scene.control.skin.ButtonSkin
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import javafx.scene.shape.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for ImplUtils
 *
 * Covers both getChildren overloads: null node / non-Parent node / Pane / Group / Control with
 * SkinBase skin / Control with non-SkinBase skin / Control without skin / plain Parent subclass.
 *
 * @author K
 * @since 1.0.0
 */
internal class ImplUtilsTest {

    @Test
    fun nullNodeYieldsEmptyList() = runFx {
        assertTrue(ImplUtils.getChildren(null as Node?).isEmpty())
    }

    @Test
    fun nonParentNodeYieldsEmptyList() = runFx {
        val node: Node = Rectangle(1.0, 1.0)
        assertTrue(ImplUtils.getChildren(node).isEmpty())
    }

    @Test
    fun nodeOverloadDelegatesToParentOverloadForPane() = runFx {
        val child = Rectangle(1.0, 1.0)
        val node: Node = VBox(child)
        val children = ImplUtils.getChildren(node)
        assertEquals(1, children.size)
        assertSame(child, children[0])
    }

    @Test
    fun nullParentYieldsEmptyObservableList() = runFx {
        assertTrue(ImplUtils.getChildren(null as Parent?).isEmpty())
    }

    @Test
    fun paneChildrenAreReturned() = runFx {
        val c1 = Rectangle()
        val c2 = Rectangle()
        val pane: Parent = Pane(c1, c2)
        val children = ImplUtils.getChildren(pane)
        assertEquals(listOf<Node?>(c1, c2), children)
    }

    @Test
    fun groupChildrenAreReturned() = runFx {
        val c1 = Rectangle()
        val group: Parent = Group(c1)
        val children = ImplUtils.getChildren(group)
        assertEquals(listOf<Node?>(c1), children)
    }

    @Test
    fun controlWithoutSkinYieldsEmptyList() = runFx {
        val button: Parent = Button("b")
        assertTrue(ImplUtils.getChildren(button).isEmpty())
    }

    @Test
    fun controlWithSkinBaseSkinReturnsSkinChildren() = runFx {
        val button = Button("b")
        button.skin = ButtonSkin(button)
        val children = ImplUtils.getChildren(button as Parent)
        assertTrue(children.isNotEmpty(), "ButtonSkin must expose at least the labeled text node")
    }

    @Test
    fun controlWithNonSkinBaseSkinYieldsEmptyList() = runFx {
        val button = Button("b")
        button.skin = object : Skin<Button> {
            override fun getSkinnable() = button
            override fun getNode(): Node = Rectangle()
            override fun dispose() {}
        }
        assertTrue(ImplUtils.getChildren(button as Parent).isEmpty())
    }

    @Test
    fun plainParentSubclassYieldsEmptyList() = runFx {
        val parent: Parent = object : Parent() {}
        assertTrue(ImplUtils.getChildren(parent).isEmpty())
    }
}
