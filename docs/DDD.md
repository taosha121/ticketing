# 领域驱动设计（DDD）四层架构

## 为什么要分层

不分层的代码会把数据库实体、HTTP 路由处理、业务逻辑全混在一起。DDD 分层的核心思想是：**把变化频率不同的东西隔离开**。

业务规则变化慢，框架技术可以被替换，HTTP 协议细节随需求变——把这三类东西放在不同的层，改一处不影响另一处。

## 依赖方向

```
interfaces  →  application  →  domain
infrastructure  →  domain
```

外层依赖内层，内层不知道外层存在。`domain` 是最稳定的核心，不依赖任何框架。

## 四层职责

### Domain（领域层）

放业务本身：实体、值对象、领域规则，和任何技术框架无关。

这层的代码应该能在没有 Spring、没有数据库的情况下独立运行和测试。

**判断标准**：这段逻辑是"业务规则"还是"技术实现"？
- "密码不能少于8位" → 业务规则 → domain
- "JWT 有效期是24小时" → 技术实现细节 → 不属于 domain

**包含**：`User.java`（JPA 实体）、`UserRepository.java`（数据访问接口）

### Application（应用层）

编排用例，不含业务逻辑。回答的问题是："完成这个操作需要哪几步？"

调度 domain 和 infrastructure，但自己不做决策。一个 Application Service 方法通常对应一个用户用例（注册、登录、购票）。

这层应该"薄"。如果 Application Service 越来越胖，说明业务逻辑在往上渗透，应该推回 domain。

**包含**：`AuthService.java`

### Infrastructure（基础设施层）

所有"可以被替换的技术实现"：数据库访问、缓存、消息队列、第三方 API、JWT 签名、框架适配器。

domain 层定义接口，infrastructure 层提供实现。换技术选型时只改这层，domain 和 application 不动。

**包含**：`JwtService.java`（JWT 签发/验证）、`UserDetailsServiceImpl.java`（Spring Security 适配器）

#### 关于 UserDetailsServiceImpl

Spring Security 定义了 `UserDetailsService` 接口，要求实现 `loadUserByUsername()`。这个类的唯一职责是把咱的 `User` 实体翻译成 Spring Security 认识的 `UserDetails` 格式——本质是个适配器，把 `UserRepository.findByEmail()` 包了一层。

方法名叫 `loadUserByUsername` 而不是 `loadUserByEmail`，是因为 Spring Security 用通用名词 `username`，实际传什么字段由实现决定，咱这里 username 就是 email。

### Interfaces（接口层）

把外部请求翻译成 application 能理解的调用，再把结果翻译回外部格式。

"外部"不只是 HTTP——消息队列消费者、定时任务、gRPC 接口都属于这层，都在做协议适配。

DTO 也在这层，因为 DTO 是 HTTP 协议的概念，domain 实体不应该直接暴露给外部。

**包含**：`AuthController.java`、`RegisterRequest.java`、`LoginRequest.java`、`AuthResponse.java`

## 一句话总结

| 层 | 问的问题 |
|---|---|
| domain | 业务规则是什么？ |
| application | 完成这个操作要做哪几步？ |
| infrastructure | 数据怎么存、怎么取、怎么发？ |
| interfaces | 外部请求怎么进来、结果怎么出去？ |

## 项目目录结构约定

咱项目按**模块 > 层**组织：

```
auth/
  domain/         User.java, UserRepository.java
  application/    AuthService.java
  infrastructure/ JwtService.java, UserDetailsServiceImpl.java
  interfaces/     AuthController.java, dto/

event/            （后续模块同结构）
  domain/
  application/
  infrastructure/
  interfaces/
```

每个业务模块内部都是这四层。小模块如果某层暂时没有内容，可以先不建该目录。

## 常见妥协点

`UserRepository` 接口放在 `domain` 层而非 `infrastructure` 层——严格 DDD 应该接口在 domain、实现在 infrastructure，但 Spring Data JPA 的便利写法让大多数项目接受这个妥协。
