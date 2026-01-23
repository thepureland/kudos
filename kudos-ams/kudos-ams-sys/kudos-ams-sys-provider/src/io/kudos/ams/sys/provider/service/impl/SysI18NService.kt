package io.kudos.ams.sys.provider.service.impl

import io.kudos.ams.sys.provider.service.iservice.ISysI18nService
import io.kudos.ams.sys.provider.model.po.SysI18n
import io.kudos.ams.sys.provider.dao.SysI18nDao
import io.kudos.ams.sys.common.vo.i18n.SysI18nRecord
import io.kudos.ams.sys.common.vo.i18n.SysI18nPayload
import io.kudos.ams.sys.common.vo.i18n.SysI18nSearchPayload
import io.kudos.base.bean.BeanKit
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ability.data.rdb.ktorm.service.BaseCrudService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


/**
 * 国际化业务
 *
 * @author K
 * @since 1.0.0
 */
@Service
//region your codes 1
open class SysI18NService : BaseCrudService<String, SysI18n, SysI18nDao>(), ISysI18nService {
//endregion your codes 1

    //region your codes 2

    private val log = LogFactory.getLog(this)

    override fun getI18nValue(locale: String, moduleCode: String, i18nTypeDictCode: String, key: String): String? {
        val searchPayload = SysI18nSearchPayload().apply {
            this.locale = locale
            this.moduleCode = moduleCode
            this.i18nTypeDictCode = i18nTypeDictCode
            this.key = key
            this.active = true
        }
        @Suppress("UNCHECKED_CAST")
        val records = dao.search(searchPayload) as List<SysI18nRecord>
        return records.firstOrNull()?.value
    }

    override fun getI18nsByModuleAndType(moduleCode: String, i18nTypeDictCode: String, locale: String?): List<SysI18nRecord> {
        val searchPayload = SysI18nSearchPayload().apply {
            this.moduleCode = moduleCode
            this.i18nTypeDictCode = i18nTypeDictCode
            this.locale = locale
        }
        @Suppress("UNCHECKED_CAST")
        return dao.search(searchPayload) as List<SysI18nRecord>
    }

    @Transactional
    override fun batchSaveOrUpdate(i18ns: List<SysI18nPayload>): Int {
        var count = 0
        i18ns.forEach { payload ->
            if (payload.id.isNullOrBlank()) {
                val i18n = SysI18n {
                    this.locale = payload.locale!!
                    this.moduleCode = payload.moduleCode!!
                    this.i18nTypeDictCode = payload.i18nTypeDictCode!!
                    this.key = payload.key!!
                    this.value = payload.value!!
                    this.active = payload.active ?: true
                }
                dao.insert(i18n)
                count++
            } else {
                val i18n = SysI18n {
                    this.id = payload.id
                    this.locale = payload.locale!!
                    this.moduleCode = payload.moduleCode!!
                    this.i18nTypeDictCode = payload.i18nTypeDictCode!!
                    this.key = payload.key!!
                    this.value = payload.value!!
                    this.active = payload.active ?: true
                }
                if (dao.update(i18n)) {
                    count++
                }
            }
        }
        log.debug("批量保存或更新国际化内容，期望处理${i18ns.size}条，实际处理${count}条。")
        return count
    }

    @Transactional
    override fun updateActive(id: String, active: Boolean): Boolean {
        val i18n = SysI18n {
            this.id = id
            this.active = active
        }
        val success = dao.update(i18n)
        if (success) {
            log.debug("更新id为${id}的国际化内容的启用状态为${active}。")
        } else {
            log.error("更新id为${id}的国际化内容的启用状态为${active}失败！")
        }
        return success
    }

    //endregion your codes 2

}