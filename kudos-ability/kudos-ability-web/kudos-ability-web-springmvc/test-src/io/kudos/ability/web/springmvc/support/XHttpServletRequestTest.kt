package io.kudos.ability.web.springmvc.support

import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [jakarta.servlet.http.HttpServletRequest] 扩展函数的单元测试。
 *
 * UA 解析是启发式的，覆盖几条主流分支足以回归——发现误判后请增删 case。
 */
internal class XHttpServletRequestTest {

    private fun reqWithUA(ua: String?): MockHttpServletRequest = MockHttpServletRequest().apply {
        if (ua != null) addHeader("User-Agent", ua)
    }

    @Test
    fun browser_chrome() {
        val ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        val (name, version) = reqWithUA(ua).getBrowserInfo()
        assertEquals("Chrome", name)
        assertEquals("120.0.0.0", version)
    }

    @Test
    fun browser_firefox() {
        val (name, version) = reqWithUA("Mozilla/5.0 (X11; Linux x86_64; rv:115.0) Gecko/20100101 Firefox/115.0").getBrowserInfo()
        assertEquals("Firefox", name)
        assertEquals("115.0", version)
    }

    @Test
    fun browser_edge_currentlyDetectedAsChrome() {
        // Edge UA 也含 "Chrome"，当前 when 顺序会优先匹配 Chrome——记录在此防止悄悄改坏；
        // 如果将来调整 when 顺序（让 Edge 先匹配），请同步更新此 assertion
        val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edge/120.0"
        val (name, _) = reqWithUA(ua).getBrowserInfo()
        assertEquals("Chrome", name)
    }

    @Test
    fun browser_emptyAgent_returnsUnknown() {
        val (name, version) = reqWithUA(null).getBrowserInfo()
        assertEquals("unknown", name)
        assertEquals("unknown", version)
    }

    @Test
    fun os_macintosh() {
        val (name, _) = reqWithUA("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)").getOsInfo()
        assertEquals("Mac", name)
    }

    @Test
    fun os_androidApp() {
        val (name, _) = reqWithUA("kudos/1.0 app_android device").getOsInfo()
        assertEquals("app_android", name)
    }

    @Test
    fun os_windowsWithVersion() {
        val (name, version) = reqWithUA("Mozilla/5.0 (Windows NT 10.0; Win64; x64)").getOsInfo()
        assertEquals("Windows", name)
        assertEquals("NT 10.0", version)
    }

    @Test
    fun terminal_mobileVsApp() {
        assertEquals("PC", reqWithUA("Mozilla/5.0 (Windows NT 10.0)").getClientTerminal())
        assertEquals("Mobile", reqWithUA("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0)").getClientTerminal())
        assertEquals("App", reqWithUA("kudos/1.0 app_ios device").getClientTerminal())
        assertEquals("unknown", reqWithUA(null).getClientTerminal())
    }

    @Test
    fun rootPath_andDomainPath() {
        val req = MockHttpServletRequest().apply {
            scheme = "https"
            serverName = "kudos.example"
            serverPort = 443
            requestURI = "/api/v1/users"
            contextPath = "/api"
        }
        assertEquals("https://kudos.example/api", req.getRootPath())
        assertEquals("https://kudos.example", req.getDomainPath())
    }
}
