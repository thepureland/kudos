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
    fun browser_blankAgent_returnsUnknown() {
        // Present-but-blank header: covers the second clause of `agent.isNullOrBlank()` (blank, not null).
        val (name, version) = reqWithUA("   ").getBrowserInfo()
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

    // region getBrowserInfo remaining branches

    @Test
    fun browser_msie_withVersion() {
        val (name, version) = reqWithUA("Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1)").getBrowserInfo()
        assertEquals("MSIE", name)
        assertEquals("8.0", version)
    }

    @Test
    fun browser_opera_versionFromVersionToken() {
        val (name, version) = reqWithUA("Opera/9.80 (Windows NT 6.1) Presto/2.12.388 Version/12.16").getBrowserInfo()
        assertEquals("Opera", name)
        assertEquals("12.16", version)
    }

    @Test
    fun browser_safari_versionFromVersionToken() {
        val ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Version/16.1 Safari/605.1.15"
        val (name, version) = reqWithUA(ua).getBrowserInfo()
        assertEquals("Safari", name)
        assertEquals("16.1", version)
    }

    @Test
    fun browser_androidApp_andIosApp() {
        assertEquals("Android App", reqWithUA("kudos/1.0 app_android device").getBrowserInfo().first)
        assertEquals("IOS App", reqWithUA("kudos/1.0 app_ios device").getBrowserInfo().first)
    }

    @Test
    fun browser_trident_ie11() {
        val (name, version) = reqWithUA("Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko").getBrowserInfo()
        assertEquals("Trident", name)
        assertEquals("unknown", version)
    }

    @Test
    fun browser_edge_withoutChromeToken() {
        val (name, _) = reqWithUA("Mozilla/5.0 (Windows NT 10.0) Edge/18.18363").getBrowserInfo()
        assertEquals("Edge", name)
    }

    @Test
    fun browser_chineseBrowsers() {
        assertEquals("Maxthon", reqWithUA("Mozilla/5.0 Maxthon/5.2").getBrowserInfo().first)
        assertEquals("qqbrowser", reqWithUA("Mozilla/5.0 qqbrowser/10.0").getBrowserInfo().first)
        assertEquals("lbbrowser", reqWithUA("Mozilla/5.0 lbbrowser").getBrowserInfo().first)
        assertEquals("UCBrowser", reqWithUA("Mozilla/5.0 UCBrowser/13.0").getBrowserInfo().first)
        assertEquals("360SE", reqWithUA("Mozilla/5.0 360SE").getBrowserInfo().first)
    }

    @Test
    fun browser_unrecognizedAgent_returnsUnknown() {
        val (name, version) = reqWithUA("curl/8.4.0").getBrowserInfo()
        assertEquals("unknown", name)
        assertEquals("unknown", version)
    }

    // endregion

    // region getOsInfo remaining branches

    @Test
    fun os_emptyAgent_returnsUnknown() {
        val (name, version) = reqWithUA(null).getOsInfo()
        assertEquals("unknown", name)
        assertEquals("unknown", version)
    }

    @Test
    fun os_blankAgent_returnsUnknown() {
        // Present-but-blank header: covers the blank clause of `agent.isNullOrBlank()` in getOsInfo.
        val (name, version) = reqWithUA("   ").getOsInfo()
        assertEquals("unknown", name)
        assertEquals("unknown", version)
    }

    @Test
    fun os_windowsWithoutParsableVersion_keepsUnknownVersion() {
        val (name, version) = reqWithUA("SomeAgent Windows").getOsInfo()
        assertEquals("Windows", name)
        assertEquals("unknown", version)
    }

    @Test
    fun os_freeBsd_solaris_iosApp() {
        assertEquals("FreeBSD", reqWithUA("Mozilla/5.0 (FreeBSD amd64)").getOsInfo().first)
        assertEquals("Solaris", reqWithUA("Mozilla/5.0 (SunOS 5.11)").getOsInfo().first)
        assertEquals("app_ios", reqWithUA("kudos/1.0 app_ios device").getOsInfo().first)
    }

    @Test
    fun os_android_unix_ios() {
        assertEquals("Android", reqWithUA("Mozilla/5.0 (Android 14; Mobile)").getOsInfo().first)
        // Note: the heuristic matches lowercase "x11"/"unix" only.
        assertEquals("Unix", reqWithUA("Mozilla/5.0 (x11; some unix)").getOsInfo().first)
        assertEquals("Unix", reqWithUA("Mozilla/5.0 (some unix)").getOsInfo().first)
        assertEquals("ios", reqWithUA("Mozilla/5.0 (iPhone; CPU iPhone OS 16_0)").getOsInfo().first)
        assertEquals("ios", reqWithUA("Mozilla/5.0 (iPad; CPU OS 16_0)").getOsInfo().first)
    }

    @Test
    fun os_linux_withAndWithoutUbuntu() {
        val (linuxName, linuxVersion) = reqWithUA("Mozilla/5.0 (Linux x86_64)").getOsInfo()
        assertEquals("Linux", linuxName)
        assertEquals("unknown", linuxVersion)

        val (ubuntuName, ubuntuVersion) = reqWithUA("Mozilla/5.0 (Linux; Ubuntu 22.04)").getOsInfo()
        assertEquals("Linux", ubuntuName)
        assertEquals("Ubuntu", ubuntuVersion)
    }

    @Test
    fun os_unrecognizedAgent_returnsUnknown() {
        assertEquals("unknown", reqWithUA("curl/8.4.0").getOsInfo().first)
    }

    // endregion

    // region getClientTerminal remaining branches

    @Test
    fun terminal_blankAgent_returnsUnknown() {
        // Present-but-blank header: covers the blank clause of `agent.isNullOrBlank()` in getClientTerminal.
        assertEquals("unknown", reqWithUA("   ").getClientTerminal())
    }

    @Test
    fun terminal_androidAppAndOtherMobiles() {
        assertEquals("App", reqWithUA("kudos/1.0 app_android device").getClientTerminal())
        assertEquals("Mobile", reqWithUA("Mozilla/5.0 (Android 14; Mobile)").getClientTerminal())
        assertEquals("Mobile", reqWithUA("Mozilla/5.0 (iPad; CPU OS 16_0)").getClientTerminal())
        assertEquals("Mobile", reqWithUA("Mozilla/5.0 (Windows Phone 10.0)").getClientTerminal())
        assertEquals("Mobile", reqWithUA("Mozilla/5.0 (BlackBerry; U)").getClientTerminal())
        assertEquals("Mobile", reqWithUA("Mozilla/5.0 (SymbianOS/9.4)").getClientTerminal())
    }

    // endregion

    // region getRemoteIp

    private fun reqWithIpHeaders(remote: String = "127.0.0.1", vararg headers: Pair<String, String>) =
        MockHttpServletRequest().apply {
            remoteAddr = remote
            headers.forEach { (name, value) -> addHeader(name, value) }
        }

    @Test
    fun remoteIp_publicXForwardedFor() {
        assertEquals("8.8.8.8", reqWithIpHeaders(headers = arrayOf("x-forwarded-for" to "8.8.8.8")).getRemoteIp())
    }

    @Test
    fun remoteIp_unknownXForwardedFor_fallsToProxyClientIp() {
        val req = reqWithIpHeaders(
            headers = arrayOf(
                "x-forwarded-for" to "unknown",
                "Proxy-Client-IP" to "9.9.9.9"
            )
        )
        assertEquals("9.9.9.9", req.getRemoteIp())
    }

    @Test
    fun remoteIp_blankHeaders_fallToWlProxyClientIp() {
        val req = reqWithIpHeaders(
            headers = arrayOf(
                "x-forwarded-for" to " ",
                "Proxy-Client-IP" to "UNKNOWN",
                "WL-Proxy-Client-IP" to "7.7.7.7"
            )
        )
        assertEquals("7.7.7.7", req.getRemoteIp())
    }

    @Test
    fun remoteIp_noHeaders_usesRemoteAddr() {
        assertEquals("6.6.6.6", reqWithIpHeaders(remote = "6.6.6.6").getRemoteIp())
        assertEquals("127.0.0.1", reqWithIpHeaders(remote = "127.0.0.1").getRemoteIp())
    }

    @Test
    fun remoteIp_multiProxyList_picksFirstPublicAddress() {
        val req = reqWithIpHeaders(
            headers = arrayOf("x-forwarded-for" to "10.0.0.1, 192.168.1.5, 8.8.4.4, 1.1.1.1")
        )
        assertEquals("8.8.4.4", req.getRemoteIp())
    }

    @Test
    fun remoteIp_allPrivateAddresses_returnsLastSegment() {
        val req = reqWithIpHeaders(
            headers = arrayOf("x-forwarded-for" to "10.255.255.255, 172.16.0.1, 192.168.0.10")
        )
        assertEquals("192.168.0.10", req.getRemoteIp())
    }

    @Test
    fun remoteIp_publicAddressAboveClassCRange_isReturned() {
        // 200.1.1.1 is numerically above the class C private range (192.168.255.255),
        // exercising the upper-bound comparison of isLocalC (the value is not A/B/C/0 -> returned as-is).
        assertEquals("200.1.1.1", reqWithIpHeaders(headers = arrayOf("x-forwarded-for" to "200.1.1.1")).getRemoteIp())
    }

    @Test
    fun remoteIp_zeroAddressIsTreatedAsLocal() {
        val req = reqWithIpHeaders(headers = arrayOf("x-forwarded-for" to "0.0.0.0, 5.5.5.5"))
        assertEquals("5.5.5.5", req.getRemoteIp())
    }

    @Test
    fun remoteIp_ipv6Loopback_translatedToLocalHostAddress() {
        val req = reqWithIpHeaders(remote = "0:0:0:0:0:0:0:1")
        val expected = java.net.InetAddress.getLocalHost().hostAddress
        assertEquals(expected, req.getRemoteIp())
    }

    // endregion
}
