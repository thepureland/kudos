# kudos-ms-auth-secret-google-secret-manager

Optional Google Secret Manager resolver for OAuth2/OIDC client secrets. It implements
`IClientSecretResolver` with the `gcp-sm:` scheme and uses the official Google Cloud Java client.

```text
gcp-sm:company-prod/kudos-auth-google
gcp-sm:company-prod/kudos-auth-google#client-secret
```

The reference contains an allowed project id/number and a flat Secret Manager secret id. It cannot contain
a version. Without `#field`, the complete non-blank UTF-8 payload is returned. With `#field`, the payload must
be a JSON object and the exact top-level textual field is returned; JSONPath and nested traversal are not
evaluated.

```properties
kudos.ms.auth.external-login.google-secret-manager.enabled=true
kudos.ms.auth.external-login.google-secret-manager.allowed-project-ids=company-prod
kudos.ms.auth.external-login.google-secret-manager.allowed-secret-id-prefixes=kudos-auth-
kudos.ms.auth.external-login.google-secret-manager.version=latest
```

Both policy lists are required for access; an empty list denies that dimension. The server-owned version is
`latest` by default and may only be changed to a positive numeric version. Provider references cannot select
disabled or historical versions.

The resolver verifies payload size, UTF-8 validity, and Google-provided CRC32C before use. Missing payload,
checksum mismatch, invalid JSON, and non-textual fields fail closed without exposing resource names or API
errors.

The host application supplies a standard `SecretManagerServiceClient` bean. Configure Application Default
Credentials, Workload Identity, service-account impersonation, endpoint, proxy, retry and RPC timeout through
the official Google client. Service-account keys must never be placed in Provider records or Kudos Web forms.
No client bean means `RESOLVER_ERROR` for `gcp-sm:` without preventing application startup. Set
`enabled=false` when another implementation owns the scheme.
