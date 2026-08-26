# kudos-ms-auth-provider-oauth2

Optional Spring Security OAuth2 Client / OIDC engine for `kudos-ms-auth`.

The module turns active `auth_identity_provider` rows into `ClientRegistration` objects at runtime.
It is intentionally separate from `kudos-ms-auth-core`: deployments that do not need browser-based
OIDC/OAuth2 login are not forced to install Spring Security's web filter infrastructure.

Client secrets are resolved through `IClientSecretResolver`; database rows contain references only.
The built-in resolver accepts the lowercase schemes `env:` and `property:` within explicit authentication
namespaces. The optional `kudos-ms-auth-secret-vault`, `kudos-ms-auth-secret-aws-secrets-manager`,
`kudos-ms-auth-secret-google-secret-manager`, and `kudos-ms-auth-secret-azure-key-vault` modules contribute
HashiCorp Vault KV, AWS Secrets Manager, Google Secret Manager, and Azure Key Vault resolvers; other secret
platforms contribute another resolver bean without changing Provider records.

## Secret reference governance

The built-in defaults permit `env:KUDOS_AUTH_*` and
`property:kudos.ms.auth.external-secrets.*`. This prevents a Provider administrator from repurposing an
unrelated process setting, such as a database password, as an OAuth client secret. Prefix lists are
deployment policy and an empty list denies that source completely:

```properties
kudos.ms.auth.external-login.allowed-secret-environment-prefixes=KUDOS_AUTH_
kudos.ms.auth.external-login.allowed-secret-property-prefixes=kudos.ms.auth.external-secrets.
```

Resolver selection is deterministic: no match, multiple matches, policy denial, missing values, and backend
errors produce distinct internal statuses. Runtime registration still fails closed and exposes none of the
backend detail. Successful value caching is local and disabled by default, so ordinary Vault/KMS or
environment rotation is immediately visible. It may be enabled for an expensive backend for at most one
hour:

```properties
kudos.ms.auth.external-login.secret-cache-ttl-seconds=0
```

When this optional module is installed, it also contributes two Auth Admin diagnostics:

```text
POST /api/admin/auth/identityProviderSecret/verify
     auth:identity-provider-secret:verify
POST /api/admin/auth/identityProviderSecret/refresh
     auth:identity-provider-secret:refresh
```

Both accept only `providerId` and a required reason. Tenant and actor come from the authenticated
administrator, and the stored reference is loaded server-side. `verify` always performs a live probe;
`refresh` first invalidates local registry/resolver state and then probes. The response contains only the
Provider id, reference scheme, status, and check time—never the path, value, length, digest, exception, or
backend metadata. With opt-in caching in a multi-node deployment, refresh each node after an urgent rotation;
the default zero TTL has no cross-node stale state.

## Browser flow

1. Create a Kudos authentication transaction for the tenant.
2. Redirect the browser to
   `/api/public/auth/external/{providerId}/authorize?transactionId={transactionId}`.
3. The module validates tenant/provider ownership, adds PKCE S256, and stores an expiring one-time
   correlation for the generated OAuth `state`.
4. Spring Security validates the authorization response, token and OIDC nonce. Kudos then maps the
   principal to `ExternalPrincipal`; an existing binding logs in directly, an unbound
   `INVITE_ONLY` principal atomically consumes a transaction-pinned invitation, and `JIT_CREATE`
   atomically provisions an external-only local account plus its first binding.
5. On success, the transaction is completed, the servlet Session ID is rotated, and a logical Kudos
   authentication session is registered with the federated `amr/acr`. Only that non-bearer logical id
   is attached to the public transaction; the servlet id remains in its HttpOnly cookie. The provider
   principal, access token and refresh token are discarded from the long-lived Kudos session.

General verified-email matching remains deliberately disabled. Redirects use the fixed local paths
`kudos.ms.auth.external-login.success-path` and `failure-path`; request parameters
cannot supply arbitrary return URLs.

Both the one-time Kudos `state` correlation and Spring Security's authorization-request snapshot are
Redis-backed when Redis is available. The latter includes the PKCE verifier and OIDC nonce, uses a
SHA-256 state key, expires after five minutes by default, and is atomically read-and-deleted by the callback.
Authorization can therefore start on one node and finish on another without sticky sessions. A deployment
without Redis falls back to an in-memory store and must keep both legs on the same process.

```properties
kudos.ms.auth.external-login.authorization-request-ttl-seconds=300
```

The accepted range is 1–900 seconds. The authorization snapshot itself does not create an HTTP session;
the local Kudos session is established only after successful external authentication.

## Typed claim mapping

Each Provider may own one typed mapping row. Every field is an ordered list of safe dot-separated paths;
the first non-null value wins. Nested maps and numeric list segments are supported, for example
`profile.username` and `profiles.0.email`. The resolver does not evaluate JSONPath, SpEL, scripts, functions,
or arbitrary code.

The mapping covers subject, username, display name, email and verification status, phone and verification
status, avatar, locale, and union id. Existing deployments retain canonical defaults when no row exists.
OIDC subject is always Spring's protocol-validated `OidcUser.subject` and the management service permits only
`sub`; OAuth2 providers use their configured subject path. Email mapping never enables implicit account merge.

## Explicit link and unlink

Self-service binding is deliberately separate from login. All identity values originate from the verified
OAuth2/OIDC callback; the public request never supplies a trusted `userId`, issuer, subject or email.

```text
GET    /api/auth/external/{providerId}/link
GET    /api/auth/external/bindings
DELETE /api/auth/external/bindings/{bindingId}
```

Link and unlink read only the authoritative logical authentication session verified for the current request.
They require password/federated assurance or stronger, and its `authTime` must be within
`kudos.ms.auth.external-login.link-reauthentication-max-age-seconds` (default: 300). A failure returns the
standard HTTP 403 Step-up challenge; after Step-up elevates the same session, the client retries the original
request. The legacy `reauthenticationTransactionId` query parameter remains accepted but ignored during its
deprecation window. Link additionally requires the Provider instance to use `link_policy=MANUAL_CONFIRM`; it creates a
`LINK_EXTERNAL_IDENTITY` transaction pinned to the current user and Provider before redirecting.

The callback binds `(tenantId, providerId, issuer, subject)`, rotates the Session ID and restores only the
minimal local principal. Unlink is a soft disable and is rejected when it would remove a passwordless
user's last active login method. Bind/unbind successes and denials are written to an append-only audit
table whose subject value is hashed.

Redirect targets are fixed configuration, validated as local paths:

```properties
kudos.ms.auth.external-login.success-path=/
kudos.ms.auth.external-login.failure-path=/
kudos.ms.auth.external-login.link-success-path=/
kudos.ms.auth.external-login.link-failure-path=/
kudos.ms.auth.external-login.link-reauthentication-max-age-seconds=300
```

## Invite-only first binding

Administrators create or revoke an invitation through Auth Admin. The invitation targets one existing
local user, one tenant and one Provider instance, expires explicitly and is one-time. Its 256-bit raw
token is returned only by the create response; storage contains SHA-256 only. An optional expected email
also uses hashed storage and requires the upstream principal to assert `email_verified=true`.

The browser starts with:

```text
GET /api/public/auth/external/{providerId}/authorize?transactionId=...&invitationToken=...
```

The raw invitation token is validated and only a secret-free invitation id is retained on the server-side authentication
transaction and is not forwarded to the upstream Provider. Callback consumption is a conditional SQL
update scoped by tenant, Provider, active state, expiry and remaining uses. It shares the transaction with
the first `user_account_third` bind, so a binding conflict rolls consumption back. Existing bindings do not
need an invitation.

Ingress/access logging must redact the `invitationToken` query parameter and analytics must not receive the
full invitation URL.

## JIT account creation

For `jit_policy=JIT_CREATE`, an unbound `(tenantId, providerId, issuer, subject)` creates an active local
account with no local password and a mandatory `user_account_third` binding. Email is never used to merge
an existing account. The generated username is bounded to 32 characters and combines a normalized readable
prefix with a stable identity hash suffix.

Provisioning runs in an independent transaction. Database uniqueness chooses one winner for concurrent
callbacks; the losing account insert/bind rolls back, after which authentication reloads the committed
binding. Successful first binding is audited as `JIT_BIND`. Provider-specific organization, account type,
status, locale, IANA timezone, currency and supervisor defaults come from the Provider-scoped JIT
configuration. A configured default organization also creates the `user_org_user` membership in the same
provisioning transaction.

The Provider may choose `EXTERNAL_USERNAME_HASHED`, `EMAIL_LOCAL_PART_HASHED`, or `OPAQUE_HASHED`.
Verified-email admission can be required independently or by an exact/wildcard domain allowlist. Domain
rules are IDN-normalized; `*.example.com` matches subdomains but not the apex. Email is admission/profile
data only and never causes an implicit merge with an existing local account.
