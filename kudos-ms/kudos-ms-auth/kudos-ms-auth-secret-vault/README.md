# kudos-ms-auth-secret-vault

Optional HashiCorp Vault KV resolver for OAuth2/OIDC client secrets. It implements the
`IClientSecretResolver` SPI from `kudos-ms-auth-provider-oauth2`; Provider rows still contain references
only, and this module never owns a Vault token.

References use a server-pinned mount and latest-version read:

```text
vault:oauth/tenant-a/google#client-secret
vault:oauth/tenant-a/line
```

The part after `vault:` is a relative KV path. `#field` is optional and defaults to `client-secret`.
References cannot select a mount or KV version and do not accept absolute paths, `.`/`..`, backslashes,
query strings, percent encoding, or whitespace.

```properties
kudos.ms.auth.external-login.vault.enabled=true
kudos.ms.auth.external-login.vault.mount=secret
kudos.ms.auth.external-login.vault.backend-version=2
kudos.ms.auth.external-login.vault.allowed-path-prefixes=oauth
kudos.ms.auth.external-login.vault.default-key=client-secret
```

An empty `allowed-path-prefixes` list denies every Vault reference. KV v1 reads
`<mount>/<relative-path>`; KV v2 reads `<mount>/data/<relative-path>` and unwraps its `data` map.

The host application must supply a standard Spring Vault `VaultOperations` bean. Configure Vault address,
TLS trust, namespace, authentication renewal and HTTP connect/read timeouts through Spring Vault or Spring
Cloud Vault. Token, AppRole secret id, Kubernetes service-account token, client certificate, and cloud IAM
credentials must not be placed in Provider records or Kudos Web configuration forms.

When no `VaultOperations` bean exists, the resolver reports the safe `RESOLVER_ERROR` status and dynamic
client registration fails closed. Set `enabled=false` when another implementation owns the `vault:` scheme.
The shared registry cache is disabled by default; `/api/admin/auth/identityProviderSecret/refresh` clears its
local value before a live read after rotation.
