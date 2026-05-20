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

import java.util.ArrayList
import java.util.Optional

/**
 * 线性 [Wizard.Flow] 实现：按构造时给定的顺序串行前进。
 * 大多数"配置 → 选项 → 完成"式向导直接复用本实现即可。
 *
 * @param pages 顺序固定的页面集合（可能为 null 占位，advance 时直接通过）
 * @author K
 * @since 1.0.0
 */
class LinearWizardFlow(pages: Collection<Wizard.WizardPane?>?) : Wizard.Flow {

    /** 内部不可变拷贝，避免外部修改原集合影响向导流程 */
    private val pages: List<Wizard.WizardPane?>

    /**
     * 便捷构造：vararg 形式直接传入若干页。
     *
     * @param pages 页面顺序列表
     * @author K
     * @since 1.0.0
     */
    constructor(vararg pages: Wizard.WizardPane) : this(listOf(*pages))

    /**
     * 推进到下一页：取当前页在列表中的位置 +1。
     *
     * @param currentPage 当前页（null 视为索引 -1，会推进到第一页）
     * @return 下一页；若已是最后一页，会抛 [IndexOutOfBoundsException]——由 [canAdvance] 兜底判断
     * @author K
     * @since 1.0.0
     */
    override fun advance(currentPage: Wizard.WizardPane?): Optional<Wizard.WizardPane> {
        var pageIndex = pages.indexOf(currentPage)
        return Optional.ofNullable(pages[++pageIndex])
    }

    /**
     * 判断是否还有下一页。
     *
     * @param currentPage 当前页
     * @return true 表示后面仍有页面可用
     * @author K
     * @since 1.0.0
     */
    override fun canAdvance(currentPage: Wizard.WizardPane?): Boolean {
        val pageIndex = pages.indexOf(currentPage)
        return pages.size - 1 > pageIndex
    }

    init {
        this.pages = ArrayList(pages)
    }
}