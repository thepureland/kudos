# kudos-ms-auth-token-jwt

可选的 Kudos JWT Access Token 适配层。core 负责逻辑会话与 Refresh Token family，本模块只负责
JWT 签发、验签、Bearer Filter 和公共 token API。

默认关闭：

```properties
kudos.ms.auth.token.jwt.enabled=true
kudos.ms.auth.token.jwt.issuer=kudos
kudos.ms.auth.token.jwt.audience=kudos-api
kudos.ms.auth.token.jwt.access-ttl-seconds=300
kudos.ms.auth.token.jwt.signing-algorithm=RS256
# 多把活动签名密钥并行轮换时设置
kudos.ms.auth.token.jwt.key-id=current-signing-key
kudos.ms.auth.token.refresh-ttl-seconds=2592000
```

启用时应用必须提供 `JwtEncoder` 和 `JwtDecoder` Bean，并使用持久化、可轮换的非对称密钥。
模块不会生成临时密钥，也不会回退到内置口令或弱共享密钥。

接口：

- `POST /api/public/auth/token`：把当前已认证逻辑 Session 换成独立 API Session 与 token pair；
- `POST /api/public/auth/token/refresh`：单次 Refresh Token 轮换；
- `POST /api/public/auth/token/revoke`：撤销 token family 与对应 API Session，响应不泄露令牌是否存在。

Access Token 包含 `sid/tenant_id/pv/auth_time/amr/acr`。每次 Bearer 请求除签名、issuer、audience
和时间窗口外，还会校验实时权限版本与逻辑会话状态；验证成功的权威逻辑会话会写入当前 request，
供 `@RequiresAuthenticationAssurance` 与浏览器 Session 链路执行完全相同的 ACR/新鲜度判断。
