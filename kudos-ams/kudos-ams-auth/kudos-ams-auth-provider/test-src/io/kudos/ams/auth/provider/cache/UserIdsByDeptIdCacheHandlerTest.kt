package io.kudos.ams.auth.provider.cache

import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import org.springframework.beans.factory.annotation.Autowired
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


/**
 * junit test for UserIdsByDeptIdCacheHandler
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserIdsByDeptIdCacheHandlerTest : CacheHandlerTestBase() {

    @Autowired
    private lateinit var cacheHandler: UserIdsByDeptIdCacheHandler

    @Test
    fun getUserIds() {
        // 存在的部门（有多个用户）
        var deptId = "11111111-1111-1111-1111-111111111111"
        val userIds2 = cacheHandler.getUserIds(deptId)
        val userIds3 = cacheHandler.getUserIds(deptId)
        assertTrue(userIds2.isNotEmpty())
        assertEquals(userIds2, userIds3)

        // 不存在的部门
        deptId = "no_exist_dept"
        val userIds4 = cacheHandler.getUserIds(deptId)
        assertTrue(userIds4.isEmpty())
    }

}
