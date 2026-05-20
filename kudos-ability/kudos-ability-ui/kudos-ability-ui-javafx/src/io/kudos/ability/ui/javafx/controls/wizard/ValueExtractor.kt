/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package io.kudos.ability.ui.javafx.controls.wizard

import javafx.scene.Node
import javafx.scene.control.*
import javafx.util.Callback


/**
 * 把 JavaFX 控件按类型映射到"读取当前值"的回调，向导各页用统一接口取值无需关心具体控件类型。
 *
 * 内置常见控件：CheckBox/ChoiceBox/ComboBox/DatePicker/PasswordField/RadioButton/Slider/TextArea/TextField/
 * ListView/TreeView/TableView/TreeTableView。业务侧需要新控件时调 [addValueExtractor] 自行注册。
 *
 * @author Oracle (原始) / K (适配)
 * @since 1.0.0
 */
object ValueExtractor {

    /** 控件类 → 取值回调；按运行时 `n.javaClass` 精确匹配，不做继承体系扩展 */
    private val valueExtractors = mutableMapOf<Class<*>, Callback<Any, Any>>()

    /**
     * 注册一种控件类型的取值回调。
     * 内部把 [Callback]'s 泛型擦除存为 `Callback<Any, Any>`，调用时再按 [Node.javaClass] 做精确分发。
     *
     * @param T 控件类型
     * @param clazz 控件 Class 对象，决定 [getValue] 时的精确匹配 key
     * @param extractor 把该类型控件转为业务值的回调
     * @author K
     * @since 1.0.0
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> addValueExtractor(clazz: Class<T>, extractor: Callback<T, Any>) {
        valueExtractors[clazz] = extractor as Callback<Any, Any>
    }

    /**
     * 尝试从给定控件读取业务值。
     * 通过 [valueExtractors] 精确匹配 `n.javaClass`；
     * 未注册的控件类型返回 null（业务侧需自行调用 [addValueExtractor] 扩展支持）。
     *
     * @param n 待取值的控件
     * @return 控件当前值；未注册类型返回 null
     * @throws IllegalArgumentException 当 map 内已注册但回调被异常清空时（不应出现）
     * @author K
     * @since 1.0.0
     */
    fun getValue(n: Node): Any? {
        var value: Any? = null
        if (valueExtractors.containsKey(n.javaClass)) {
            val callback = requireNotNull(valueExtractors[n.javaClass]) { "No extractor for ${n.javaClass}" }
            value = callback.call(n)
        }
        return value
    }

    init {
        addValueExtractor(CheckBox::class.java) { cb: CheckBox -> cb.isSelected }
        addValueExtractor(ChoiceBox::class.java) { cb: ChoiceBox<*> -> cb.value }
        addValueExtractor(ComboBox::class.java) { cb: ComboBox<*> -> cb.value }
        addValueExtractor(DatePicker::class.java) { dp: DatePicker -> dp.value }
        addValueExtractor(PasswordField::class.java) { pf: PasswordField -> pf.text }
        addValueExtractor(RadioButton::class.java) { rb: RadioButton -> rb.isSelected }
        addValueExtractor(Slider::class.java) { sl: Slider -> sl.value }
        addValueExtractor(TextArea::class.java) { ta: TextArea -> ta.text }
        addValueExtractor(TextField::class.java) { tf: TextField -> tf.text }
        addValueExtractor(ListView::class.java) { lv: ListView<*> ->
            val sm = lv.selectionModel
            if (sm.selectionMode == SelectionMode.MULTIPLE) sm.selectedItems else sm.selectedItem
        }
        addValueExtractor(TreeView::class.java) { tv: TreeView<*> ->
            val sm: MultipleSelectionModel<*> = tv.selectionModel
            if (sm.selectionMode == SelectionMode.MULTIPLE) sm.selectedItems else sm.selectedItem
        }
        addValueExtractor(TableView::class.java) { tv: TableView<*> ->
            val sm: MultipleSelectionModel<*> = tv.selectionModel
            if (sm.selectionMode == SelectionMode.MULTIPLE) sm.selectedItems else sm.selectedItem
        }
        addValueExtractor(TreeTableView::class.java) { tv: TreeTableView<*> ->
            val sm: MultipleSelectionModel<*> = tv.selectionModel
            if (sm.selectionMode == SelectionMode.MULTIPLE) sm.selectedItems else sm.selectedItem
        }
    }
}
