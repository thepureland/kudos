# kudos-ms-auth-secret-azure-key-vault

Optional Azure Key Vault resolver for OAuth2/OIDC client secrets. It implements `IClientSecretResolver`
with the `azure-kv:` scheme and uses the official Azure SDK for Java.

```text
azure-kv:company-prod/kudos-auth-google
azure-kv:company-prod/kudos-auth-google#client-secret
```

The reference contains a deployment-owned vault alias and a flat Key Vault secret name; it never contains
a vault URL or version. Without `#field`, the complete non-blank string is returned. With `#field`, the value
must be a JSON object and the exact top-level textual field is returned; JSONPath and nested traversal are
not evaluated.

```properties
kudos.ms.auth.external-login.azure-key-vault.enabled=true
kudos.ms.auth.external-login.azure-key-vault.vault-urls.company-prod=https://company-prod.vault.azure.net/
kudos.ms.auth.external-login.azure-key-vault.allowed-secret-name-prefixes=kudos-auth-
kudos.ms.auth.external-login.azure-key-vault.version=
```

The vault alias map and secret-name prefixes are both deployment policy; an empty map or list denies that
dimension. Every configured URL must be an exact HTTPS vault root without user info, port, query or fragment.
The resolver only selects a supplied `SecretClient` whose `getVaultUrl()` matches that normalized URL, so a
Provider reference cannot create clients or select an arbitrary network endpoint. Multiple Azure public,
sovereign-cloud, private-endpoint, or tenant vaults can be represented by separate aliases and clients.

An empty server-owned version reads the latest version for normal rotation. A pinned value must be a
32-character hexadecimal Azure version id. Provider references cannot select versions.
Missing/blank values, oversized values, invalid JSON, non-textual fields, duplicate/missing matching clients,
and Azure API failures fail closed without exposing vault URLs, secret names, request ids, or API errors.

The host application supplies one standard `SecretClient` bean per configured vault URL. Configure
`DefaultAzureCredential`, Managed Identity, Workload Identity, service-principal federation, sovereign cloud
authority, private endpoint, proxy, retry and HTTP timeout through the official Azure clients. Credentials must
never be placed in Provider records or Kudos Web forms. Set `enabled=false` when another implementation owns
the scheme.
