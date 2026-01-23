package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysDictItemI18nService
import io.kudos.ams.sys.provider.model.po.SysDictItemI18n
import io.kudos.ams.sys.provider.dao.SysDictItemI18nDao
import io.kudos.ams.sys.common.vo.dictitemi18n.SysDictItemI18nRecord
import io.kudos.ams.sys.common.vo.dictitemi18n.SysDictItemI18nPayload
import io.kudos.ams.sys.common.vo.dictitemi18n.SysDictItemI18nSearchPayload
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 字典项国际化业务
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysDictItemI18NService : BaseCrudService<String, SysDictItemI18n, SysDictItemI18nDao>(), ISysDictItemI18nService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    /**
     * 获取字典项的所有国际化内容
     *
     * @param itemId 字典项id
     * @return 国际化记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getI18nsByItemId(itemId: String): List<SysDictItemI18nRecord> {
        val searchPayload = SysDictItemI18nSearchPayload().apply {
            this.itemId = itemId
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysDictItemI18nRecord>
    }

    /**
     * 根据字典项id和语言地区获取国际化值
     *
     * @param itemId 字典项id
     * @param locale 语言地区
     * @return 国际化值，找不到返回null
     * @author AI: Cursor
     * @since 1.0.0
     */
    override fun getI18nValue(itemId: String, locale: String): String? {
        val searchPayload = SysDictItemI18nSearchPayload().apply {
            this.itemId = itemId
            this.locale = locale
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysDictItemI18nRecord>
        return records.firstOrNull()?.i18nValue
    }

    /**
     * 批量保存或更新国际化内容
     *
     * @param itemId 字典项id
     * @param i18ns 国际化载体列表
     * @return 成功保存或更新的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun batchSaveOrUpdate(itemId: String, i18ns: List<SysDictItemI18nPayload>): Int {
        var count = 0
        i18ns.forEach { payload ->
            if (payload.id.isNullOrBlank()) {
                val i18n = SysDictItemI18n {
                    this.itemId = itemId
                    this.locale = payload.locale!!
                    this.i18nValue = payload.i18nValue!!
                    this.active = payload.active ?: true
                }
                dao.insert(i18n)
                count++
            } else {
                val i18n = SysDictItemI18n {
                    this.id = payload.id
                    this.itemId = itemId
                    this.locale = payload.locale!!
                    this.i18nValue = payload.i18nValue!!
                    this.active = payload.active ?: true
                }
                if (dao.update(i18n)) {
                    count++
                }
            }
        }
        log.debug("批量保存或更新字典项${itemId}的国际化内容，期望处理${i18ns.size}条，实际处理${count}条。")
        return count
    }

    /**
     * 删除字典项的所有国际化内容
     *
     * @param itemId 字典项id
     * @return 删除的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun deleteByItemId(itemId: String): Int {
        val criteria = Criteria.of(SysDictItemI18n::itemId.name, OperatorEnum.EQ, itemId)
        val count = dao.batchDeleteCriteria(criteria)
        log.debug("删除字典项${itemId}的所有国际化内容，共删除${count}条。")
        return count
    }

    /**
     * 更新启用状态
     *
     * @param id 国际化id
     * @param active 是否启用
     * @return 是否更新成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val i18n = SysDictItemI18n {
            this.id = id
            this.active = active
        }
        val success = dao.update(i18n)
        if (success) {
            log.debug("更新id为${id}的字典项国际化内容的启用状态为${active}。")
        } else {
            log.error("更新id为${id}的字典项国际化内容的启用状态为${active}失败！")
        }
        return success
    }

    //endregion your codes 2

}