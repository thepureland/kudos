package io.kudos.ability.file.minio.client

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.minio.credentials.Jwt
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * 锁定 [AccessTokenMinioClientBuilder] 里那个 Jackson 3 mapper 配置对 [io.minio.credentials.Jwt]
 * 反序列化的行为。
 *
 * 背景：Minio 的 `Jwt` 是一个 immutable 类（两个 private final 字段、无无参构造），Jackson
 * 默认按 getter/setter 找不到入口。OIDC token 解析能跑通的关键就是 mapper 上的
 * `changeDefaultVisibility { withFieldVisibility(ANY) }` —— 直接写私有字段。
 *
 * 把 Jackson 2 `ObjectMapper().setVisibility(...)` 改成 Jackson 3 `JsonMapper.builder().changeDefaultVisibility(...)`
 * 之后这个路径理论上等价，但 Jackson 3 几乎重写了 builder/visibility API，等价性不能"看着像"就放过。
 * 任何回归（builder API 改名 / visibility 解析语义变化 / FAIL_ON_UNKNOWN 默认变化）都会让这条
 * STS 集成路径在线上启动后才挂。本测试在编译期 + 单测期就把它锁住。
 */
internal class AccessTokenJwtMapperTest {

    /** 与 AccessTokenMinioClientBuilder.accessToken() 内部完全相同的 mapper 构造。 */
    private val mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultVisibility { it.withFieldVisibility(JsonAutoDetect.Visibility.ANY) }
        .build()

    @Test
    fun deserializesMinioJwtFromStandardOAuthResponse() {
        // Jwt 的两个字段通过 @ConstructorProperties + @JsonProperty 绑定到 OAuth2 标准键名
        // access_token / expires_in（参见 io.minio.credentials.Jwt 字节码）。
        val json = """{"access_token":"eyJhbGciOiJIUzI1NiJ9.payload.sig","expires_in":3600}"""

        val jwt = mapper.readValue(json, Jwt::class.java)

        assertNotNull(jwt)
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", jwt.token())
        assertEquals(3600, jwt.expiry())
    }

    @Test
    fun ignoresUnknownPropertiesFromTokenServer() {
        // 真实 OIDC token server 顺带回 refresh_token / scope / token_type 等额外字段。
        // mapper 必须放过它们，否则 Jwt 反序列化会因为 FAIL_ON_UNKNOWN_PROPERTIES 抛异常。
        val json = """
            {
              "access_token": "abc",
              "expires_in": 1800,
              "refresh_token": "y",
              "scope": "read write",
              "token_type": "Bearer"
            }
        """.trimIndent()

        val jwt = mapper.readValue(json, Jwt::class.java)

        assertEquals("abc", jwt.token())
        assertEquals(1800, jwt.expiry())
    }
}
