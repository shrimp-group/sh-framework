# sh-framework Mega Skill 实现计划 v2

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）逐任务实现。步骤使用复选框（`- [ ]`）语法跟踪进度。

**目标：** 创建单一的 `sh-framework` skill，内部包含索引 SKILL.md 和 13 个子 skill 目录，每个子目录有独立 SKILL.md。

**架构：** 在 `.trae/skills/sh-framework/` 下创建 1 个索引文件 + 13 个子目录（各含 SKILL.md），更新 `superpowers-zh.md` 注册条目。

**技术栈：** Markdown

---

### 任务 1：创建索引 SKILL.md

**文件：**
- 创建：`.trae/skills/sh-framework/SKILL.md`

- [ ] **步骤 1：创建文件**

```markdown
---
name: sh-framework
description: sh-framework 框架知识总索引——涉及框架编码、模块选择、依赖管理时调用。自动触发做轻量提示，手动调用展开完整索引。
---

# sh-framework 框架知识总索引

## 核心规则

1. 当用户涉及 sh-framework 框架编码时，自动加载此 skill，根据用户意图匹配子技能
2. 当用户显式调用 `sh-framework` 时，展开完整索引并引导选择
3. 此 SKILL.md 只做路由——具体知识在子目录 `sh-*/SKILL.md` 中
4. 匹配到子技能后，读取对应子目录的 `SKILL.md` 获取知识

## 可用子技能

| 子技能 | 路径 | 适用场景 |
|-------|------|---------|
| sh-tool | `sh-tool/SKILL.md` | 涉及加密（AES/DES/RSA/MD5/SHA/Base64）、字符串格式化、日期、Bean 操作、文件 IO、网络、雪花 ID、验证码、二维码、JS 引擎等工具类操作时 |
| sh-core | `sh-core/SKILL.md` | 涉及实体体系（DbColumnEntity/BaseEntity/PageData/UserInfo）、异常体系（CommonException 及 7 个子类）、ResultCode 枚举、R 统一响应、UserContext 用户上下文、自定义注解时 |
| sh-mybatis | `sh-mybatis/SKILL.md` | 涉及数据库操作、Mapper 编写、SQL 生成、BaseMapper/BaseService 使用、拦截器、逻辑删除、乐观锁、@Blob 大字段、分页查询时 |
| sh-spring | `sh-spring/SKILL.md` | 涉及 Spring 上下文获取 Bean、雪花 ID 生成、邮件发送、FreeMarker 模板渲染、系统环境判断、敏感配置加解密时 |
| sh-redis | `sh-redis/SKILL.md` | 涉及 Redis 缓存操作（String/Hash/List/Set/ZSet）、分布式锁、ID 生成、消息队列时 |
| sh-web | `sh-web/SKILL.md` | 涉及全局异常处理、IP 解析、请求/响应工具、REST 接口元数据扫描、用户名自动填充、@AtLeastOneNotNull 校验时 |
| sh-dynamicdb | `sh-dynamicdb/SKILL.md` | 涉及多数据源切换、运行时动态添加/销毁数据源、租户隔离数据源时 |
| sh-mqtt | `sh-mqtt/SKILL.md` | 涉及 MQTT 消息收发、IoT 设备通信、@MqttController 注解驱动订阅/分发时 |
| sh-xxljob | `sh-xxljob/SKILL.md` | 涉及 XXL-Job 定时任务开发、@XxlJob 注解任务处理器编写时 |
| sh-iam-contract-api | `sh-iam-contract-api/SKILL.md` | 涉及 IAM 契约接口（AuthContract/AuthzContract/AkSignContract/SsoFacadeContract）、Principal/Session/AuthResult 等模型、PrincipalContext、LoginFailType 枚举、AuthException 异常时 |
| sh-iam-contract-default | `sh-iam-contract-default/SKILL.md` | 涉及 IAM 默认实现（DefaultAuthFilter）、@ConditionalOnMissingBean 替换默认实现、IAM 自动配置时 |
| sh-bom | `sh-bom/SKILL.md` | 涉及依赖版本管理、新增第三方依赖、版本冲突排查时 |
| sh-demo | `sh-demo/SKILL.md` | 需要参考框架标准使用范式（Entity→Mapper→Service→VO→Route→Controller CRUD 全链路）时 |

## 模块依赖层级

```
第0层: sh-tool（无内部依赖）
第1层: sh-core → sh-tool
第2层: sh-mybatis → sh-core, sh-spring → sh-core
第3层: sh-dynamicdb → sh-mybatis + sh-spring
       sh-redis → sh-core
       sh-web / sh-mqtt / sh-xxljob → sh-spring
       sh-iam-contract（api 零内部依赖；default → api）
第4层: sh-demo → sh-mybatis + sh-web
```

## 触发行为

### 自动触发（轻量模式）

当用户消息涉及 sh-framework 框架编码时：

1. 扫描用户意图，匹配子技能适用场景
2. 匹配到 1-2 个 → 直接读取对应子目录 SKILL.md
3. 匹配模糊 → 简短提示调用 `sh-framework` 查看完整索引

### 手动调用（完整模式）

用户显式 `Use Skill: sh-framework` 时，展示完整索引表和依赖层级，引导选择子技能。
```

- [ ] **步骤 2：Commit**

```bash
git add .trae/skills/sh-framework/SKILL.md
git commit -m "feat: add sh-framework mega skill index"
```

---

### 任务 2：创建第0-1层子技能（sh-tool, sh-core）

**文件：**
- 创建：`.trae/skills/sh-framework/sh-tool/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-core/SKILL.md`

- [ ] **步骤 1：创建 sh-tool/SKILL.md**

```markdown
# sh-tool 工具类模块

## 概述

sh-tool 是 sh-framework 最底层模块，无框架内部依赖。提供加密、字符串、日期、Bean、文件、网络、雪花ID、验证码、二维码、JS引擎等通用工具类。

## 关键工具类

| 类名 | 用途 |
|------|------|
| `StringFormat` | 字符串模板格式化，支持 `{}` 占位符和 `${var}` 命名变量 |
| `StringUtil` | 字符串工具（下划线/驼峰转换等） |
| `BeanUtil` | Bean 拷贝（cpAll / cpNotNull / removeBlank） |
| `DateUtil` | 日期工具 |
| `JsonUtil` | JSON 工具（基于 fastjson2） |
| `MapUtil` | Map 工具（obj2Map） |
| `FileUtil` | 文件 IO 工具 |
| `SnowflakeIdWorker` | 雪花 ID 算法 |
| `NetworkUtil` | 网络工具（获取服务器 IP 等） |
| `SecretUtil` | 加密工具入口 |
| `QrCodeUtil` | 二维码生成 |
| `ValidateCode` | 验证码生成 |
| `JsUtil` | JS 脚本引擎 |
| `ClassUtil` | 类扫描工具 |
| `CompressUtil` | 压缩工具 |
| `CheckPwdUtil` | 密码强度检查 |
| `AesTool` | AES 对称加密 |
| `DesTool` | DES 对称加密 |
| `RsaTool` | RSA 非对称加密 |
| `Md5Tool` | MD5 摘要 |
| `ShaTool` | SHA 摘要 |
| `Base64Tool` | Base64 编码 |

## 依赖

无框架内部依赖，仅依赖三方库（lombok、fastjson2、guava、hutool、zxing 等）。
```

- [ ] **步骤 2：创建 sh-core/SKILL.md**

```markdown
# sh-core 核心模块

## 概述

sh-core 是 sh-framework 核心基础模块，依赖 sh-tool。包含实体体系、异常体系、结果码、统一响应、用户上下文、自定义注解。

## 实体体系

- `DbColumnEntity`：数据库规范字段基类（id, sort, createTime, createBy, updateTime, updateBy, remark, version）
- `BaseEntity extends DbColumnEntity`：业务实体基类，增加分页/查询辅助字段（createByName, updateByName, userCode, tenantCode, orderBy, ids, keyword, timeFrom, timeTo, current, size, offset, total, count, debug）
  - 提供 `copy()` / `copyIfNotNull()` 静态方法
- `Pageable`：分页接口，`DEFAULT_CURRENT=1`, `DEFAULT_SIZE=10`，`init()` 校验并计算 offset
- `PageData<T>`：泛型分页封装，工厂方法 `fromEntity()` / `of()` / `empty()` / `convert()`
- `UserInfo`：登录用户信息（userCode, username, nickname, mobile[脱敏], tenantCode, avatar, openId）
- **所有业务实体必须继承 BaseEntity**

## 异常体系

- `CommonException`：业务异常基类（继承 RuntimeException），持有 `code` 字段
- 构造器：`(String)`, `(ResultCode)`, `(int, String)`, `(String, Throwable)`, `(ResultCode, Throwable)`, `(int, String, Throwable)`
- 静态工厂：`of(String message, Object... args)` 使用 `StringFormat.of()` 模板格式化
- 7 个子类：`ApiException` / `ApplicationException` / `NotFoundException` / `SystemException` / `UnauthorizedException` / `UserException` / `ValidationException`

## 返回结果

- `R<T>`：统一响应，字段：code, msg, data, requestTime, responseTime, costTime
- 使用：`R.ok(data)`, `R.warn(message)`, `R.error(message)`, `R.error(code, message)` 等
- warn → 400，error → 500

## 结果码

- `ResultCode` 枚举：200/400/401/403/404/500
- 业务码段：10001-10102 Token/登录, 20001-20004 跨域/路由, 30001-30005 登录/验证码, 40001-40006 数据操作, 50001-50003 网络, 60001-60003 订单

## 用户上下文

- `UserContext`：`ThreadLocal<UserInfo>`，方法：`setUserInfo()` / `getUserInfo()` / `getUserCode()` / `getTenantCode()` / `clear()`

## 自定义注解

- `@Router(module, prefix)`：TYPE 作用域，路由标识，定义模块和前缀
- `UserNameProvider`：SPI 接口，供 `UserNameBodyAdvice` 自动填充 createByName / updateByName

## 依赖

sh-tool
```

- [ ] **步骤 3：Commit**

```bash
git add .trae/skills/sh-framework/sh-tool/SKILL.md .trae/skills/sh-framework/sh-core/SKILL.md
git commit -m "feat: add sh-tool and sh-core sub-skills"
```

---

### 任务 3：创建第2层子技能（sh-mybatis, sh-spring）

**文件：**
- 创建：`.trae/skills/sh-framework/sh-mybatis/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-spring/SKILL.md`

- [ ] **步骤 1：创建 sh-mybatis/SKILL.md**

```markdown
# sh-mybatis MyBatis ORM 模块

## 概述

sh-mybatis 提供通用 Mapper/Service、动态 SQL 生成、MyBatis 拦截器链、逻辑删除、乐观锁等数据库操作能力。依赖 sh-core。

## BaseMapper

- `BaseMapper<T extends BaseEntity>`：通用 Mapper 接口，14 个方法
- 所有 SQL 由 Provider 动态生成（`@XxxProvider` 注解），不使用 XML 映射

## BaseService

- `BaseService<T, M>`：通用 Service 抽象类，`@Service` + `@Transactional`
- `BATCH_SIZE=1000`
- `selectPage()` 内部使用 `selectCountByEntity` + `selectByEntityWithLimit`

## MyBatis 拦截器链（执行顺序：Query → Update → BoundSql）

- `MyBatisUpdateInterceptor`：自动填充 createBy / updateBy（从 UserContext），清空 createTime / updateTime
- `MyBatisQueryInterceptor`：空字符串 → null
- `MyBatisBoundSqlInterceptor`：向 BoundSql 注入 updateBy 参数

## 逻辑删除与乐观锁

- 逻辑删除：deleted 字段，`buildWhereClause()` 自动追加 `deleted = 0`
- 乐观锁：version 字段，更新时自动追加 `version = version + 1`
- **不要在业务 SQL 中手动添加这些条件**

## @Blob 注解

- `@Blob`（FIELD 作用域）：标记 Blob 字段，List 查询时不返回（selectListFields 排除）

## 分页与排序

- `PageQuery`：分页查询工具（支持 BaseEntity 和 Pageable 接口）
- `buildOrderByClause()`：白名单校验防止 SQL 注入
- **不要在 Service 层手动调用 PageHelper.startPage()**

## 已知注意事项

- `DeleteByIdMapperProvider` 和 `DeleteByIdsMapperProvider` 中 `#{updateBy}` 必须显式 `javaType=String`，否则类型推断错误

## 依赖

sh-core
```

- [ ] **步骤 2：创建 sh-spring/SKILL.md**

```markdown
# sh-spring Spring 扩展模块

## 概述

sh-spring 提供 Spring 上下文持有器、雪花 ID、邮件发送、模板渲染、敏感配置加解密等 Spring 扩展能力。依赖 sh-core。

## 关键类

| 类名 | 用途 |
|------|------|
| `SpringContextHolder` | Spring 上下文全局持有器（静态获取 Bean） |
| `SnowflakeHelper` | 雪花 ID 辅助类（workId = 网卡 hashCode % 31） |
| `Sys` | 系统初始化与环境管理 |
| `SystemConfig` | 系统配置 |
| `MailUtil` | 邮件发送（HTML / 内嵌图片 / 附件） |
| `FreeMarkerTemplateUtil` | FreeMarker 模板渲染 |
| `SensitiveConfigEncryptor/Decryptor` | 敏感配置加解密 |

## 敏感配置

支持三种模式：
- RSA 密钥库：`sh.config.keystore.path/alias/password`
- AES 对称密钥：`sh.config.decrypt-aes-key`
- 明文（不推荐）

敏感值使用 `ENC(...)` 包裹。

## 告警邮件

配置前缀 `alarm.email.*`，ErrorHandler 异常时触发邮件告警。

## 依赖

sh-core
```

- [ ] **步骤 3：Commit**

```bash
git add .trae/skills/sh-framework/sh-mybatis/SKILL.md .trae/skills/sh-framework/sh-spring/SKILL.md
git commit -m "feat: add sh-mybatis and sh-spring sub-skills"
```

---

### 任务 4：创建第3层子技能（sh-dynamicdb, sh-redis, sh-web, sh-mqtt, sh-xxljob, sh-iam-contract-api, sh-iam-contract-default）

**文件：**
- 创建：`.trae/skills/sh-framework/sh-dynamicdb/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-redis/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-web/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-mqtt/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-xxljob/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-iam-contract-api/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-iam-contract-default/SKILL.md`

- [ ] **步骤 1：创建 sh-dynamicdb/SKILL.md**

```markdown
# sh-dynamicdb 动态数据源模块

## 概述

sh-dynamicdb 实现运行时动态切换数据源。基于 AbstractRoutingDataSource 扩展，支持运行时添加/销毁数据源、DCL 双重检查锁缓存、异步创建防死循环、AOP 自动清理 ThreadLocal。依赖 sh-mybatis + sh-spring。

## 核心类

| 类名 | 用途 |
|------|------|
| `DynamicDataSource` | 核心数据源（DCL + 异步创建 + 定时清理） |
| `DynamicDataSourceHolder` | ThreadLocal 管理当前数据源 key |
| `DynamicDataSourceFactory` | 数据源工厂接口 |
| `DynamicDataSourceAop` | AOP 切面，Mapper 方法后自动清理 ThreadLocal |
| `AbstractShrimpRoutingDataSource` | 扩展 AbstractRoutingDataSource |

## 配置

- `sh.dynamicdb.cache-second`：数据源缓存时间（秒，默认 60）
- `sh.dynamicdb.cleanup-interval-second`：清理间隔（秒，默认 120）

## 注意事项

- **不要手动调用 `DynamicDataSourceHolder.clear()`**，AOP 自动处理
- 数据源创建使用 CompletableFuture 异步，防止死循环

## 依赖

sh-mybatis, sh-spring
```

- [ ] **步骤 2：创建 sh-redis/SKILL.md**

```markdown
# sh-redis Redis 模块

## 概述

sh-redis 提供 Redis 全数据类型缓存操作、分布式锁、ID 生成、消息队列。依赖 sh-core。

## 关键类

| 类名 | 用途 |
|------|------|
| `RedisHelper` | Redis 全数据类型操作（String/Hash/List/Set/ZSet） |
| `RedisLock` | 分布式锁（SETNX + Lua 原子释放 + Watchdog 自动续期） |
| `LockHolder` | 锁持有器 |
| `RedisIdGenerator` | ID 生成器（时间戳 + 机器标识 + Redis 自增，Base62 编码） |
| `RedisMessageQueue` | Redis List 消息队列 |
| `Fastjson2JsonRedisSerializer` | Redis 序列化器 |

## 配置

- `sh.redis.*`：Redis 配置前缀

## 注意事项

- **RedisHelper 所有方法 try-catch 返回默认值（false/null/0），不向上抛出**
- 如果业务需要感知 Redis 异常，需自行处理

## 依赖

sh-core
```

- [ ] **步骤 3：创建 sh-web/SKILL.md**

```markdown
# sh-web Web 模块

## 概述

sh-web 提供全局异常处理、IP 解析、请求/响应工具、REST 接口元数据扫描、用户名自动填充、自定义参数校验。依赖 sh-spring。

## 全局异常处理

- `ErrorHandler`：全局处理 8 种异常类型
- UserException 只记 biz error 日志不发邮件
- 其他异常触发邮件告警

## 关键类

| 类名 | 用途 |
|------|------|
| `IpHelper` | IP 地址解析（x-forwarded-for 链路） |
| `RequestHelper` | 请求工具（路径匹配、域名解析） |
| `ResponseHelper` | 响应工具（错误写入、Excel 流式输出） |
| `RestHelper` | REST 接口元数据扫描（参数信息、返回类型、泛型信息） |
| `LocalThreadHelper` | 线程上下文（ThreadLocal<ConcurrentHashMap>） |

## 用户名自动填充

- `UserNameBodyAdvice`：ResponseBodyAdvice，自动填充 createByName / updateByName
- 通过 SPI 接口 `UserNameProvider`（sh-core）实现

## 标准 Bean

| 类名 | 用途 |
|------|------|
| `IdReq` | ID 请求 |
| `RemoveReq` | 删除请求 |
| `UpdateReq` | 更新请求（继承 id + version） |
| `PageReq` | 分页请求（继承 current + size + offset） |
| `EntityResp` | 实体响应（继承审计字段） |
| `RestInfo` | REST 接口信息 |

## 注解

- `@AtLeastOneNotNull`：TYPE 作用域，类级校验指定字段至少一个非空
  - **被校验字段不应同时使用 @NotNull，否则冲突**

## 配置

- `sh.swagger.*`：Swagger API 文档配置

## 依赖

sh-spring
```

- [ ] **步骤 4：创建 sh-mqtt/SKILL.md**

```markdown
# sh-mqtt MQTT 消息模块

## 概述

sh-mqtt 基于 Eclipse Paho MQTT v3，提供注解驱动消息处理（@MqttController + @MqttTopicMapping）、消息发布、SSL/TLS 单向认证、断线自动重连重订阅。依赖 sh-spring。

## 注解

| 注解 | 作用域 | 用途 |
|------|--------|------|
| `@MqttController` | TYPE | MQTT 消息处理器（含 @Component） |
| `@MqttTopicMapping` | METHOD | 订阅子 Topic |

## 关键类

| 类名 | 用途 |
|------|------|
| `MqttProducer` | MQTT 消息发布（即时 / 延时 / 批量） |
| `MqttSubscribe` | 订阅分发 |
| `MqttBeanPostProcessor` | Bean 后处理器，注册 MQTT 处理器 |
| `MqttHandlerFactory` | 处理器工厂（注册/查找） |
| `MqttMessage` | 远程调用请求消息 |
| `MqttResponse` | 远程调用响应 |
| `MqttHexMsg` | 十六进制消息 Bean |

## 异常

- `MqttBeansException` / `MqttRemoteException` / `MqttSendException` / `MqttTimeoutException`

## 配置

- `shrimp.cloud.mqtt.*`（注意：不是 `sh.mqtt.*`）

## 依赖

sh-spring
```

- [ ] **步骤 5：创建 sh-xxljob/SKILL.md**

```markdown
# sh-xxljob XXL-Job 定时任务模块

## 概述

sh-xxljob 提供 XXL-Job 执行器自动配置，基于 xxl-job-core，支持 @XxlJob 注解开发任务处理器。依赖 sh-spring。

## 关键类

| 类名 | 用途 |
|------|------|
| `XxlJobConfig` | XXL-Job 配置类 |
| `XxlJobAutoConfigure` | 自动配置（注意命名不一致，非 ShXxlJobAutoConfig） |
| `XxlJobDemo` | 示例任务处理器 |

## 配置

- `sh.xxl-job.*`：XXL-Job 配置前缀
- `spring.application.name`：appName 默认取此值

## 注意事项

- 需要 XXL-Job Admin 服务端

## 依赖

sh-spring
```

- [ ] **步骤 6：创建 sh-iam-contract-api/SKILL.md**

```markdown
# sh-iam-contract-api IAM 契约 API 模块

## 概述

sh-iam-contract-api 是 IAM 契约层 API 模块，零业务依赖（仅 spring-boot-starter-web provided）。提供认证/鉴权/AK 签名/SSO 门面四契约 SPI、中性模型和 PrincipalContext。

## 契约接口

| 接口 | 用途 |
|------|------|
| `AuthContract` | 认证契约 SPI（authenticate + checkToken） |
| `AuthzContract` | 鉴权契约 SPI（租户/应用/菜单/接口/字段/数据六维度，含上下文重载） |
| `AkSignContract` | AK 签名契约 SPI（sign + verifySign） |
| `SsoFacadeContract` | SSO 门面契约 SPI（login + saveLog + logout） |

## 核心模型

| 类 | 用途 |
|------|------|
| `Principal` | 认证主体 |
| `Session` | 会话信息 |
| `AuthResult` | 认证结果 |
| `Tenant` / `App` / `Menu` / `Api` | 租户/应用/菜单/接口模型 |
| `FieldPermission` / `DataDimension` | 字段权限/数据维度 |
| `LoginResp` | 登录响应（含失败建模：success + failType + failReason + 静态工厂） |
| `SessionCreateReq` | 会话创建请求 |

## 枚举

| 枚举 | 用途 |
|------|------|
| `AuthScene` | 认证场景 |
| `LoginFailType` | 登录失败类型（10 值 + 中文 message，枚举内完成翻译；USERNAME_OR_PASSWORD_ERROR 合并防枚举；UNKNOWN 兜底） |
| `AuthErrorType` | 认证错误类型（8 值 + HTTP 状态码 + 友好提示） |

## 上下文

- `PrincipalContext`：基于 RequestContextHolder + ThreadLocal 双存储
- `ContractSettings`：静态配置持有器（供 default 方法访问）

## 异常

- `AuthException`：认证异常

## 依赖

零内部依赖（仅 spring-boot-starter-web provided + lombok + swagger-annotations）
```

- [ ] **步骤 7：创建 sh-iam-contract-default/SKILL.md**

```markdown
# sh-iam-contract-default IAM 契约默认实现模块

## 概述

sh-iam-contract-default 提供 IAM 契约层的默认实现（读宽容验证严格）。依赖 iam-contract-api，通过 @ConditionalOnMissingBean 注册默认实现。

## 默认实现

| 类 | 用途 |
|------|------|
| `DefaultAuthContract` | 默认认证实现（读宽容验证严格） |
| `DefaultAuthzContract` | 默认鉴权实现（读返回空 + canAccessApi 抛 ACCESS_DENIED） |
| `DefaultAkSignContract` | 默认 AK 签名实现（功能不可用） |
| `DefaultSsoFacadeContract` | 默认 SSO 门面实现（login 抛异常 + saveLog/logout 静默） |

## 过滤器与配置

| 类 | 用途 |
|------|------|
| `DefaultAuthFilter` | 默认鉴权过滤器（调用 AuthContract SPI） |
| `IamContractAutoConfig` | 自动配置（@ConditionalOnMissingBean 注册默认实现） |
| `ContractConfig` | 配置绑定 |

## 配置

- `sh.iam.contract.enabled`：是否启用契约层自动配置（默认 true）
- `iam.contract.auth-filter-enabled`：是否注册 DefaultAuthFilter（默认 true）
- `iam.contract.public-path-pattern`：公开路径匹配模式（默认 `/*/public/**`）
- `iam.contract.app-id`：AK 签名 appId
- `iam.contract.app-secret`：AK 签名 appSecret（RSA 私钥）
- `iam.contract.public-key`：AK 验签 publicKey（RSA 公钥）
- `iam.contract.server-url`：SSO 服务端地址
- `iam.contract.jwt-secret-key`：JWT 密钥

## 依赖

iam-contract-api
```

- [ ] **步骤 8：Commit**

```bash
git add .trae/skills/sh-framework/sh-dynamicdb/SKILL.md .trae/skills/sh-framework/sh-redis/SKILL.md .trae/skills/sh-framework/sh-web/SKILL.md .trae/skills/sh-framework/sh-mqtt/SKILL.md .trae/skills/sh-framework/sh-xxljob/SKILL.md .trae/skills/sh-framework/sh-iam-contract-api/SKILL.md .trae/skills/sh-framework/sh-iam-contract-default/SKILL.md
git commit -m "feat: add layer-3 sub-skills (dynamicdb, redis, web, mqtt, xxljob, iam-contract)"
```

---

### 任务 5：创建第4层子技能 + sh-bom（sh-demo, sh-bom）

**文件：**
- 创建：`.trae/skills/sh-framework/sh-demo/SKILL.md`
- 创建：`.trae/skills/sh-framework/sh-bom/SKILL.md`

- [ ] **步骤 1：创建 sh-demo/SKILL.md**

```markdown
# sh-demo 示例模块

## 概述

sh-demo 演示基于 sh-framework 搭建 CRUD 服务的标准范式。依赖 sh-mybatis + sh-web。

## CRUD 标准范式

1. **定义实体** — `User extends BaseEntity`
2. **定义 Mapper** — `UserMapper extends BaseMapper<User>` + `@Mapper`
3. **定义 Service** — `UserService extends BaseService<User, UserMapper>` + `@Service`
4. **定义 VO 类**：
   - `UserCreateReq` — 创建请求（implements Serializable, `@NotBlank` / `@NotNull` 校验）
   - `UserUpdateReq extends UpdateReq` — 更新请求（继承 id + version）
   - `UserPageReq extends PageReq` — 分页请求（继承 current + size + offset）
   - `UserResp extends EntityResp` — 详情响应（继承审计字段）
   - `UserPageResp` — 分页列表响应
5. **定义 Route** — `@Router(module, prefix)` 接口，常量定义 URI
6. **定义 REST 控制器** — `@RestController` + `@RequestMapping(Route.PREFIX)`

## 运行

```bash
cd sh-demo
mvn spring-boot:run
```

端口 8080，profile 默认 local。

## 依赖

sh-mybatis, sh-web
```

- [ ] **步骤 2：创建 sh-bom/SKILL.md**

```markdown
# sh-bom BOM 依赖管理模块

## 概述

sh-bom 统一管理所有第三方依赖版本。无 Java 代码，仅 pom.xml 的 dependencyManagement。

## 关键约定

- 所有三方依赖版本统一在 sh-bom 中管理
- 子模块不得硬编码版本号
- 新增依赖先在 sh-bom 的 `<dependencyManagement>` 中声明版本属性

## 关键依赖

| 类别 | 技术 | 版本管理方式 |
|------|------|-------------|
| ORM | MyBatis | 4.0.1 (starter) |
| 分页 | PageHelper | 4.0.0 (starter) |
| 连接池 | Druid | 1.2.28-SNAPSHOT (Spring Boot 4 适配版) |
| 数据库 | MySQL | 9.7.0 (connector) |
| 缓存 | Redis (Lettuce) | 继承 Spring Boot BOM |
| 消息队列 | MQTT (Eclipse Paho) | 1.2.5 |
| 定时任务 | XXL-Job | 3.4.0 |
| API 文档 | Swagger Annotations v3 | 2.2.49 |
| JSON | fastjson2 | 2.0.61 |
| 工具库 | Hutool | 5.8.44 |
| 工具库 | Guava | 33.6.0-jre |
| 加密 | BouncyCastle (jdk18on) | 1.84 |
| JWT | jjwt | 0.13.0 |

## 注意事项

- 微信模块需排除旧 BouncyCastle 冲突
- Druid 使用 Spring Boot 4 适配版
- Redis 版本继承 Spring Boot BOM
- sh-mqtt 的 paho.mqttv3 版本直接写在 pom.xml 中，未使用 sh-bom 定义的 `${mqttv3.version}` 属性（已知问题）
```

- [ ] **步骤 3：Commit**

```bash
git add .trae/skills/sh-framework/sh-demo/SKILL.md .trae/skills/sh-framework/sh-bom/SKILL.md
git commit -m "feat: add sh-demo and sh-bom sub-skills"
```

---

### 任务 6：更新 `superpowers-zh.md`

**文件：**
- 修改：`.trae/rules/superpowers-zh.md`

- [ ] **步骤 1：在表格末尾（writing-skills 行之后，`## 如何使用` 行之前）插入一行**

```
| sh-framework | 框架知识总索引——涉及框架编码、模块选择、依赖管理时调用。包含 13 个子技能（sh-tool/sh-core/sh-mybatis/sh-spring/sh-redis/sh-web/sh-dynamicdb/sh-mqtt/sh-xxljob/sh-iam-contract-api/sh-iam-contract-default/sh-bom/sh-demo）。 |
```

- [ ] **步骤 2：Commit**

```bash
git add .trae/rules/superpowers-zh.md
git commit -m "docs: register sh-framework mega skill in superpowers-zh"
```

---

## 验证

1. `.trae/skills/sh-framework/SKILL.md` 存在（索引文件）
2. `.trae/skills/sh-framework/sh-*/SKILL.md` 全部 13 个子目录文件存在
3. `.trae/rules/superpowers-zh.md` 包含 sh-framework 条目
4. 现有 `.trae/skills/` 下其他 skill 文件未被修改
