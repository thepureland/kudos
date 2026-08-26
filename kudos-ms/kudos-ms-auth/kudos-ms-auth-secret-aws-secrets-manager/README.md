# kudos-ms-auth-secret-aws-secrets-manager

Optional AWS Secrets Manager resolver for OAuth2/OIDC client secrets. It implements
`IClientSecretResolver` with the `aws-sm:` scheme and uses AWS SDK v2 `SecretsManagerClient`.

```text
aws-sm:kudos/auth/tenant-a/google
aws-sm:kudos/auth/tenant-a/google#client-secret
```

Without `#field`, the complete non-blank `SecretString` is returned. With `#field`, the secret must be a
JSON object and the exact top-level textual field is returned. JSONPath, nested traversal, binary secrets,
version ids, and request-selected version stages are not supported.

```properties
kudos.ms.auth.external-login.aws-secrets-manager.enabled=true
kudos.ms.auth.external-login.aws-secrets-manager.allowed-secret-id-prefixes=kudos/auth
kudos.ms.auth.external-login.aws-secrets-manager.allow-arns=false
kudos.ms.auth.external-login.aws-secrets-manager.allowed-arn-prefixes=
kudos.ms.auth.external-login.aws-secrets-manager.version-stage=AWSCURRENT
```

Secret-name policy uses path-segment boundaries, so `kudos/auth2` does not match `kudos/auth`. Empty name
prefixes deny all name references. ARN references are disabled by default; enabling them also requires an
exact ARN or a slash-terminated ARN namespace prefix that pins partition, region, account and service.
References cannot contain traversal segments, repeated/leading slashes, whitespace, percent encoding, query
strings, or an AWS version selector.

The host application supplies a standard `SecretsManagerClient` bean. Configure region, endpoint, default
AWS credentials provider chain or workload identity, proxy, retries, and HTTP connect/read timeouts on that
client. Access keys must never be placed in Provider records or Kudos Web forms. The resolver always requests
the server-configured stage (`AWSCURRENT` by default), allowing normal Secrets Manager rotation.

Without a client bean, `aws-sm:` reports `RESOLVER_ERROR` and Provider registration fails closed without
breaking application startup. SDK messages, request ids, secret ids, ARNs and values are not returned or
logged by this module. Set `enabled=false` if another resolver owns the scheme.
