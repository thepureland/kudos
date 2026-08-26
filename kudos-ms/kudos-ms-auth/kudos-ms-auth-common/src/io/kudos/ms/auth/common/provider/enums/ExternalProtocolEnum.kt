package io.kudos.ms.auth.common.provider.enums

/** External authentication protocol families supported by the provider catalog. */
enum class ExternalProtocolEnum {
    OIDC,
    OAUTH2,
    SAML2,
    LDAP,
    CAS,
    CUSTOM,
}
