# kudos-ms-auth-session-spring

可选模块：会话被撤销的**瞬间**物理删除 servlet 容器侧的会话记录。

## 为什么是可选的

撤销本身不需要它。每个请求都会重新校验逻辑会话，所以被撤销的会话立刻失效，无论容器里还留着什么。
容器记录残留的代价是存储占用，以及**存储运维方仍能读到它的属性**——这才是把会话放在共享存储里的部署
通常希望在撤销瞬间就删掉、而不是等用户下次请求的原因。

本仓库**刻意没有替部署选择会话存储**（`spring-session-data-redis` 曾被移除，见
`kudos-ability-web-springmvc` 的构建脚本注释）。因此本模块只依赖 `spring-session-core`，具体存储
（Redis / JDBC / Hazelcast…）仍由部署自己声明。没有安装本模块的部署保持原有的惰性失效行为。

## 启用

```yaml
kudos:
  ms:
    auth:
      session:
        container-purge:
          enabled: true
```

还需要容器里存在一个 `FindByIndexNameSessionRepository` Bean。**只认按 principal 建索引的
repository**，而不是任意 `SessionRepository`：没有索引就无法找到某个用户的容器会话，而一个"总是什么都
没找到"的清理器比不装更糟——部署会以为撤销顺带清理了，实际并没有。

## 两个 id 是怎么对上的

容器会话 id 实际上就是会话 cookie，是一份**活的凭证**。把它记进 auth 的表里，等于把凭证放到管理 API
会读取的地方。所以本模块不建立这个映射，而是从**已经存在的地方**取：每个容器会话在签发时就带着逻辑
会话 id 作为属性（`AuthenticationSession.HTTP_SESSION_ATTRIBUTE`）。清理时按 principal 索引取出该用户的
容器会话，再按该属性匹配要删的那些。

签发侧还会写入一个 principal 索引属性。该属性名是 Spring Session 的
`FindByIndexNameSessionRepository.PRINCIPAL_NAME_INDEX_NAME`，在 `kudos-ms-auth-common` 里以字面量复制了
一份，这样签发会话的模块不必为一个常量引入 Spring Session 依赖；本模块的测试断言这份复制仍与上游常量
相等，避免它悄悄漂移。

## 失败语义

清理是尽力而为，**绝不向撤销路径抛异常**。撤销是安全动作且已经落库，因为存储不可达而让它失败，等于用真正
的保证去换一个装饰性的保证。清理失败只记 error，容器记录仍会在下一次请求时由 Filter 失效。

同一会话被重复撤销时会**再次**触发清理：前一次可能静默失败过，而删除一个已不存在的记录是无操作，这次重试
比它省下的一次查询更值钱。
