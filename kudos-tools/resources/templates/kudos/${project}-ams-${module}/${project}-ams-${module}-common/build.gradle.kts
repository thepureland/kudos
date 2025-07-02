dependencies {
    <#if project == "kudos">
    api(project(":kudos-base"))
    <#else>
    api("io.kudos:kudos-base")
    </#if>

    //region your codes 1

    //endregion your codes 1
}