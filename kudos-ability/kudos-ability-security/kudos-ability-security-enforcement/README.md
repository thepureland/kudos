# kudos-ability-security-enforcement

**定位**：权限的**执行层（PEP）**——把授权模型从"渲染菜单的建议"变成"真正的边界"。

菜单过滤只是把入口藏起来，直接敲 URL 照样能调。这个模块负责在请求进入应用之前拦下来，
问一次决策点，然后放行或拒绝。

---

## 依赖方向：ability 不认识 ms

模块只定义两个**端口**，自己不实现：

| 端口 | 谁来实现 | 职责 |
|---|---|---|
| `IAuthzDecisionProvider` | `kudos-ms-auth-core` | 当前调用者是否持有某个权限编码 |
| `IPermissionPointRegistry` | `kudos-ms-auth-core` | `(method, path)` → 权限编码 |

依赖方向是 **ms → ability**，刻意为之：换掉整个授权后端（外部 IAM、自研策略服务）只需换
这两个端口的实现，过滤器和注解一行不用改；同时 ability 层保持与微服务模块零依赖。

---

## 两个执行点

**`PermissionEnforcementFilter`（URL 级）** 判定顺序，每一步的理由都写在类注释里：

1. 公开路径 → 放行（**显式声明**，绝不靠"没注册"来推断）
2. 未注册权限点 → **拒绝**（本模块最承重的一条，见下）
3. 无登录主体 → 401（与"有身份但没权限"区分开，客户端才知道该重登还是该申请）
4. 决策点说不行 → 403

**`@RequiresPermission("sys:user:delete")`（方法级）** 覆盖 URL 表达不了的入口：消息消费者、
定时任务、服务间内部方法。注解上的编码必须是具体的——通配符只属于**授权**，不属于**要求**。

### 为什么"未注册 = 拒绝"

"这个接口没人注册过"和"这个接口人人可访问"是两件不同的事。如果过滤器把未注册当成放行，
那么每一次忘记注册权限点都会变成一个**静默的洞**——而且是那种上线很久都没人发现的洞。
默认拒绝会让漏注册立刻暴露成一个可见的失败，这正是影子模式存在的意义。

---

## 落地路径：影子模式

```yaml
kudos:
  ability:
    security:
      enforcement:
        enabled: true        # 默认 false
        shadow-mode: true    # 默认 true
```

- `enabled: false`（默认）：加了依赖也不改变任何行为。
- `shadow-mode: true`：只打 WARN 日志记录"本该拒绝什么"，仍然放行。

存量系统的正确顺序是：**开 enabled + 保持 shadow → 跑真实流量 → 按 WARN 日志把权限点补齐
→ 关掉 shadow**。日志刻意用 WARN 而不是 DEBUG：一个没人打开的日志级别产出的是一份空清单。

没有这个开关，接入鉴权就是一次停机事件。

---

## 权限点从哪来

`sys_resource` 是权限点注册表。`url` 字段支持两种写法：

```
/api/admin/auth/role/bindUsers          任意方法
POST:/api/admin/auth/role/bindUsers     限定方法
/api/admin/auth/role/ + 双星通配         整棵子树
```

匹配优先级固定为"限定方法 > 任意方法，同级取最长模式"——授权结果不能取决于表里的行顺序。

---

## 配置项

| 配置 | 默认 | 说明 |
|---|---|---|
| `enabled` | `false` | 是否安装过滤器 |
| `shadow-mode` | `true` | 只记录不拦截 |
| `public-paths` | `/actuator/**`、`/error` | 免鉴权路径（Ant 模式） |
| `context-headers` | `[]` | 带入条件求值上下文的请求头（`ip` 自动带入） |

---

## 依赖

- `kudos-ability-web-common` / `spring-boot-starter-web`（Servlet 过滤器）
- `spring-boot-starter-aop`（方法级注解）
