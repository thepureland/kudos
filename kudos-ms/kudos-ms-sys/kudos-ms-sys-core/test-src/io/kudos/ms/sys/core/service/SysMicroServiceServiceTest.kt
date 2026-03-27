package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.ms.sys.core.service.iservice.ISysMicroServiceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysMicroServiceService
 *
 * 测试数据来源：`SysMicroServiceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysMicroServiceService: ISysMicroServiceService

    /** 种子数据中的微服务编码（物理主键列为 code，无 id 列；实体 id 与 code 等价） */
    private val seededMicroServiceCode = "svc-microservice-test-1_2407"

    @Test
    fun get_byCodePrimaryKey_entityIdEqualsCode() {
        val row = sysMicroServiceService.get(seededMicroServiceCode)
        assertNotNull(row)
        assertEquals(seededMicroServiceCode, row.code)
        assertEquals(seededMicroServiceCode, row.id)
    }

    @Test
    fun deleteById_usesCodeColumn_notPhysicalIdColumn() {
        val unique = UUID.randomUUID().toString().replace("-", "").take(12)
        val code = "tc_del_$unique"
        val inserted = SysMicroService().apply {
            this.code = code
            name = "nm_$unique"
            context = "/test"
            atomicService = false
            parentCode = null
            remark = null
            active = true
            builtIn = false
        }
        assertEquals(code, sysMicroServiceService.insert(inserted))
        assertNotNull(sysMicroServiceService.get(code))
        assertTrue(sysMicroServiceService.deleteById(code))
        assertNull(sysMicroServiceService.get(code))
    }

    @Test
    fun getMicroServiceByCode_and_updateActive() {
        val code = seededMicroServiceCode
        val cacheItem = sysMicroServiceService.getMicroServiceByCode(code)
        assertNotNull(cacheItem)
        assertTrue(sysMicroServiceService.updateActive(code, false))
        assertTrue(sysMicroServiceService.updateActive(code, true))
    }

    @Test
    fun getAtomicServicesByMicroServiceCode() {
        val microServiceCode = seededMicroServiceCode
        val atomicServices = sysMicroServiceService.getAllActiveAtomicServiceByParentCode(microServiceCode)
        assertTrue(atomicServices.any { it.code == "svc-as-ms-test-1_2407" })
    }
}

