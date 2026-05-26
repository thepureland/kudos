package io.kudos.ability.web.springmvc.support

import org.springframework.mock.web.MockHttpServletRequest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [jakarta.servlet.http.HttpServletRequest] extension functions.
 *
 * UA parsing is heuristic; covering a few mainstream branches is enough for regression —
 * add/remove cases when misclassifications are found.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
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
        // The Edge UA also contains "Chrome"; the current `when` ordering matches Chrome first — recorded here to prevent silent regressions.
        // If the `when` ordering is changed in the future (so that Edge matches first), update this assertion accordingly.
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
