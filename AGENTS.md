# AGENTS.md

## 项目概览

sh-framework 是一个基于 Spring Boot 4.0 的 Java 后端基础框架，使用 Java 25 编译。项目采用 Maven 多模块结构，提供通用的基础设施能力，包括 ORM、缓存、MQ、动态数据源、定时任务等。

框架的核心目标是：**为业务系统提供开箱即用的基础设施层**，让业务开发者只需关注业务逻辑本身。通过 BaseEntity + BaseMapper + BaseService 的泛型体系，实现单表 CRUD 零代码；通过 MyBatis 拦截器链自动填充审计字段；通过统一响应 R + 全局异常处理 ErrorHandler 规范 API 输出；通过 SPI 机制（UserNameProvider）实现框架与业务系统的松耦合。
新增 IAM 契约层（sh-iam-contract）模块，提供认证/鉴权/AK 签名/SSO 门面四契约接口与默认实现（读宽容验证严格），业务系统可按需引入或通过 @ConditionalOnMissingBean 替换默认实现。

- **GroupId**: `com.wkclz.framework`
- **版本管理**: 使用 `${revision}` 占位符 + flatten-maven-plugin 管理版本
- **当前版本**: `5.0.1-SNAPSHOT`
- **基础包名**: `com.wkclz`

## 技术栈清单

| 类别 | 技术 | 版本 | 作用 |
|------|------|------|------|
| 语言 | Java | 25 | 编译与运行 |
| 框架 | Spring Boot | 4.0.6 | 应用框架（继承 spring-boot-starter-parent） |
| ORM | MyBatis | 4.0.1 (starter) | 数据库访问 |
| 分页 | PageHelper | 4.0.0 (starter) | MyBatis 分页插件（含自定义 PageInterceptor） |
| 连接池 | Druid | 1.2.28-SNAPSHOT (Spring Boot 4 适配版) | 数据库连接池 |
| 数据库 | MySQL | 9.7.0 (connector) | 关系型数据库 |
| 缓存 | Redis (Lettuce) | 继承 Spring Boot BOM | 分布式缓存 |
| 消息队列 | MQTT (Eclipse Paho) | 1.2.5 | IoT 消息通信 |
| 定时任务 | XXL-Job | 3.4.0 | 分布式任务调度 |
| API 文档 | Swagger Annotations (v3) | 2.2.49 | OpenAPI 接口描述 |
| JSON | fastjson2 | 2.0.61 | JSON 序列化/反序列化 |
| 工具库 | Hutool | 5.8.44 | 通用工具集 |
| 工具库 | Guava | 33.6.0-jre | Google 核心工具库 |
| 工具库 | Apache Commons (lang3/collections4) | 3.x / 4.5.0 | 基础工具 |
| 加密 | BouncyCastle (jdk18on) | 1.84 | JCE 加密扩展 |
| 模板 | FreeMarker | 继承 Spring Boot BOM | 邮件模板渲染 |
| 邮件 | Jakarta Mail | 2.0.2 | 邮件发送 |
| 二维码 | ZXing | 3.5.4 | 条码/二维码生成 |
| JWT | jjwt | 0.13.0 | Token 生成与验证 |
| JS 引擎 | Rhino | 1.9.1 | 脚本执行引擎 |
| 构建 | Maven + flatten-maven-plugin | 1.7.3 | 项目构建与版本管理 |
| 简化 | Lombok | 1.18.46 | 代码简化（@Data, @Slf4j 等） |

## 目录结构说明

```
sh-framework/
├── pom.xml                    # 根 POM，聚合所有模块
├── AGENTS.md                  # AI 代理协作指南
├── docs/
│   └── stories/               # 用户故事文档（30 个，按模块分组）
├── sh-parent/                 # 父 POM，继承 spring-boot-starter-parent，统一依赖管理
│   └── pom.xml                # import sh-bom，管理内部模块版本，定义构建插件
├── sh-bom/                    # BOM（物料清单），管理所有三方依赖版本
│   └── pom.xml                # 仅 dependencyManagement，无 Java 代码
├── sh-tool/                   # 工具类模块（无框架内部依赖，最底层）
│   └── src/main/java/com/wkclz/tool/
│       ├── tools/             # 加密工具（AES/DES/RSA/MD5/SHA/Base64/Regular）
│       └── utils/             # 通用工具（String/Date/Json/Bean/File/Network/Snowflake/QrCode/JS 等）
├── sh-core/                   # 核心模块（实体、异常、枚举、用户上下文、注解）
│   └── src/main/java/com/wkclz/core/
│       ├── annotation/        # @Router
│       ├── base/              # DbColumnEntity, BaseEntity, Pageable, PageData, R, UserInfo
│       ├── enums/             # ResultCode, EnvType
│       ├── exception/         # CommonException + 7 个子类
│       ├── log/               # MaskingPatternLayout（日志脱敏）
│       ├── spi/               # UserNameProvider（SPI 接口）
│       └── user/              # UserContext（ThreadLocal 用户上下文）
├── sh-mybatis/                # MyBatis 模块（BaseMapper、BaseService、拦截器、SQL Provider）
│   └── src/main/java/com/wkclz/mybatis/
│       ├── annotation/        # @Blob
│       ├── bean/              # DbEntityProperty, ColumnInfo, TableInfo 等元数据
│       ├── helper/            # PageQuery
│       ├── interceptor/       # MyBatisUpdateInterceptor, MyBatisQueryInterceptor, MyBatisBoundSqlInterceptor
│       ├── mapper/            # BaseMapper（14 个通用方法）
│       ├── mapper/impl/       # SQL Provider 体系（16 个 Provider 类）
│       ├── service/           # BaseService, TableInfoService
│       └── ShMyBatisAutoConfig
├── sh-spring/                 # Spring 扩展模块（上下文、配置、雪花ID、邮件）
│   └── src/main/java/com/wkclz/spring/
│       ├── config/            # SpringContextHolder, Sys, SystemConfig, SensitiveConfigEncryptor/Decryptor
│       ├── helper/            # SnowflakeHelper
│       ├── utils/             # MailUtil, FreeMarkerTemplateUtil
│       └── ShSpringAutoConfig
├── sh-dynamicdb/              # 动态数据源模块（运行时切换、DCL、异步创建）
│   └── src/main/java/com/wkclz/dynamicdb/
│       ├── aop/               # DynamicDataSourceAop（Mapper 方法后清理 ThreadLocal）
│       ├── bean/              # DefaultDataSourceConfig
│       ├── config/            # DynamicDataSourceConfig
│       ├── AbstractShrimpRoutingDataSource  # 扩展 AbstractRoutingDataSource
│       ├── DynamicDataSource              # 核心数据源（DCL + 异步创建 + 定时清理）
│       ├── DynamicDataSourceHolder         # ThreadLocal key
│       ├── DynamicDataSourceFactory        # 数据源工厂接口
│       └── ShDynamicdbAutoConfig
├── sh-redis/                  # Redis 模块（缓存、分布式锁、ID 生成、消息队列）
│   └── src/main/java/com/wkclz/redis/
│       ├── config/            # RedisConfig, RedisTemplateConfig, RedisKeepAliveConfig
│       ├── helper/            # RedisHelper, RedisLock, LockHolder, RedisIdGenerator
│       ├── queue/             # RedisMessageQueue 接口与实现
│       ├── serializer/        # Fastjson2JsonRedisSerializer
│       └── ShRedisAutoConfig
├── sh-web/                    # Web 模块（全局异常处理、IP、请求/响应工具、用户名填充）
│   └── src/main/java/com/wkclz/web/
│       ├── annotation/        # @AtLeastOneNotNull + Validator
│       ├── bean/              # IdReq, RemoveReq, UpdateReq, PageReq, EntityResp, RestInfo
│       ├── helper/            # IpHelper, RequestHelper, ResponseHelper, RestHelper, LocalThreadHelper
│       ├── rest/              # ErrorHandler, UserNameBodyAdvice
│       └── ShWebAutoConfig
├── sh-xxljob/                 # XXL-Job 定时任务模块
│   └── src/main/java/com/wkclz/xxljob/
│       ├── config/            # XxlJobConfig
│       ├── demo/              # XxlJobDemo
│       └── XxlJobAutoConfigure
├── sh-mqtt/                   # MQTT 消息模块（注解驱动发布/订阅、SSL/TLS、断线重连）
│   └── src/main/java/com/wkclz/mqtt/
│       ├── annotation/        # @MqttController, @MqttTopicMapping
│       ├── bean/              # MqttHexMsg
│       ├── client/            # MqttProducer
│       ├── config/            # MqttConfig, MqttSubscribe, MqttBeanPostProcessor, MqttApplicationListener
│       ├── demo/              # MqttProducerDemo, MqttConsumerDemo
│       ├── enums/             # Qos
│       ├── exception/         # MqttBeansException, MqttRemoteException, MqttSendException, MqttTimeoutException
│       ├── handler/           # MqttHandlerFactory
│       ├── remote/            # MqttMessage, MqttResponse
│       └── MqttAutoConfigure
├── sh-iam-contract/           # IAM 契约层模块（认证/鉴权/AK 签名/SSO 门面四契约 + 默认实现）
│   ├── iam-contract-api/      # 契约 API（零业务依赖，接口 + 中性模型 + PrincipalContext）
│   │   └── src/main/java/com/wkclz/iam/contract/
│   │       ├── bean/          # Principal, Session, AuthResult, Tenant, App, Menu, Api, FieldPermission, DataDimension, RequestLog
│   │       ├── bean/req/      # SessionCreateReq
│   │       ├── bean/resp/     # LoginResp
│   │       ├── config/        # ContractSettings（静态配置持有器）
│   │       ├── context/       # PrincipalContext（基于 RequestContextHolder + ThreadLocal 双存储）
│   │       ├── enums/         # AuthScene
│   │       ├── exception/     # AuthException（含 AuthErrorType 枚举）
│   │       ├── facade/        # SsoFacadeContract
│   │       └── service/       # AuthContract, AuthzContract, AkSignContract
│   └── iam-contract-default/  # 默认实现（读宽容验证严格 + DefaultAuthFilter + AutoConfig）
│       └── src/main/java/com/wkclz/iam/contract/defaults/
│           ├── config/        # ContractConfig, IamContractAutoConfig
│           ├── facade/       # DefaultSsoFacadeContract
│           ├── filter/       # DefaultAuthFilter
│           └── service/      # DefaultAuthContract, DefaultAuthzContract, DefaultAkSignContract
└── sh-demo/                   # 示例模块（演示框架标准使用范式）
    └── src/main/java/com/wkclz/demo/
        ├── DemoApplication.java
        ├── bean/entity/       # User (extends BaseEntity)
        ├── bean/vo/user/      # UserCreateReq, UserUpdateReq, UserPageReq, UserResp, UserPageResp
        ├── mapper/            # UserMapper (extends BaseMapper<User>)
        ├── rest/              # Route, UserRest
        └── service/           # UserService (extends BaseService<User, UserMapper>)
```

### 模块依赖关系

```
spring-boot-starter-parent:4.0.6
  │
sh-parent (import sh-bom)
  │
  ├─ sh-tool（无框架内部依赖）
  │    └─ [lombok, spring-boot-starter-logging, commons-lang3, commons-collections4,
  │       spring-beans, guava, fastjson2, zxing, hutool-all, rhino]
  │
  ├─ sh-core ──> sh-tool
  │    └─ [lombok, spring-boot-starter-test, swagger-annotations]
  │
  ├─ sh-mybatis ──> sh-core
  │    └─ [druid-spring-boot-4-starter, mysql-connector-j,
  │       mybatis-spring-boot-starter, pagehelper-spring-boot-starter, lombok]
  │
  ├─ sh-spring ──> sh-core
  │    └─ [spring-boot-starter, spring-boot-starter-freemarker,
  │       spring-boot-starter-mail, jakarta.mail, bcpkix-jdk18on]
  │
  ├─ sh-dynamicdb ──> sh-mybatis + sh-spring
  │    └─ [spring-boot-starter-aop, lombok]
  │
  ├─ sh-redis ──> sh-core
  │    └─ [spring-boot-starter-data-redis]
  │
  ├─ sh-web ──> sh-spring
  │    └─ [spring-boot-starter-web, spring-boot-starter-actuator,
  │       mysql-connector-j(optional), spring-jdbc(optional),
  │       swagger-annotations, jakarta.validation-api]
  │
  ├─ sh-iam-contract ──> spring-boot-starter-web (provided)
  │    ├─ iam-contract-api（零内部依赖，仅 spring-boot-starter-web provided + lombok + swagger-annotations）
  │    └─ iam-contract-default ──> iam-contract-api（@ConditionalOnMissingBean 注册默认实现）
  │
  ├─ sh-xxljob ──> sh-spring
  │    └─ [spring-boot-configuration-processor(optional), xxl-job-core, lombok]
  │
  ├─ sh-mqtt ──> sh-spring
  │    └─ [jakarta.annotation-api, bcprov-jdk18on, paho.mqttv3, lombok]
  │
  └─ sh-demo ──> sh-mybatis + sh-web
       └─ [spring-boot-maven-plugin (可执行 JAR)]
```

**依赖层级**：
- 第 0 层：sh-tool（无内部依赖）
- 第 1 层：sh-core（依赖 sh-tool）
- 第 2 层：sh-mybatis, sh-spring（依赖 sh-core）
- 第 3 层：sh-dynamicdb（依赖 sh-mybatis + sh-spring）, sh-redis（依赖 sh-core）, sh-web / sh-xxljob / sh-mqtt（依赖 sh-spring）, sh-iam-contract（iam-contract-api 零内部依赖；iam-contract-default 依赖 iam-contract-api）
- 第 4 层：sh-demo（依赖 sh-mybatis + sh-web）

## 核心逻辑与工作流

### 数据流转核心路径

```
HTTP Request
  → Controller（VO 校验: @Valid / @NotNull / @AtLeastOneNotNull）
  → BeanUtils.copyProperties（VO → Entity）
  → Service（BaseService 泛型 CRUD）
  → MyBatis 拦截器链
      ├─ MyBatisQueryInterceptor: 空字符串 → null
      ├─ MyBatisUpdateInterceptor: 自动填充 createBy / updateBy（从 UserContext 获取）
      └─ MyBatisBoundSqlInterceptor: 向 BoundSql 注入 updateBy 参数
  → SQL Provider（动态 SQL 生成，自动追加 deleted=0，乐观锁 version，@Blob 字段分离）
  → Database

Database
  → Entity（BaseEntity，含审计字段）
  → ResponseBodyAdvice（UserNameBodyAdvice: SPI 批量填充 createByName / updateByName）
  → BeanUtils.copyProperties（Entity → Resp VO）
  → R<VO> 统一响应
  → HTTP Response
```

### CRUD 标准范式（参考 sh-demo）

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

### 架构模式

| 模式 | 应用场景 |
|------|----------|
| 模板方法 | BaseService 定义 CRUD 骨架，子类继承即获得完整能力 |
| 策略模式 | SQL Provider 体系，每种操作一个 Provider 类，通过 `@XxxProvider` 注解引用 |
| 工厂模式 | MqttHandlerFactory 注册/查找处理器，DynamicDataSourceFactory 创建数据源 |
| 观察者模式 | MQTT 的 `@MqttController` + `@MqttTopicMapping` 注解驱动订阅/分发 |
| SPI 机制 | UserNameProvider 接口，业务系统实现，框架自动发现 |
| 代理模式 | MyBatis 拦截器链（Update/Query/BoundSql），AOP 切面（DynamicDataSourceAop） |
| Builder/工厂方法 | `R.ok()` / `R.error()` / `R.warn()`，`PageData.of()` / `fromEntity()` / `empty()` |

### 自动配置

每个模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置：

| 模块 | 自动配置类 | 备注 |
|------|-----------|------|
| sh-mybatis | `com.wkclz.mybatis.ShMyBatisAutoConfig` | 标准命名 |
| sh-spring | `com.wkclz.spring.ShSpringAutoConfig` | 标准命名 |
| sh-redis | `com.wkclz.redis.ShRedisAutoConfig` | 标准命名 |
| sh-web | `com.wkclz.web.ShWebAutoConfig` | 标准命名 |
| sh-dynamicdb | `com.wkclz.dynamicdb.ShDynamicdbAutoConfig` | 标准命名 |
| sh-xxljob | `com.wkclz.xxljob.XxlJobAutoConfigure` | 命名不一致 |
| sh-mqtt | `com.wkclz.mqtt.MqttAutoConfigure` | 命名不一致 |

> sh-tool 和 sh-core 无自动配置。

## 核心约定

### 实体体系

- `DbColumnEntity`：数据库规范字段基类（id, sort, createTime, createBy, updateTime, updateBy, remark, version）
- `BaseEntity extends DbColumnEntity`：业务实体基类，增加分页/查询辅助字段（createByName, updateByName, userCode, tenantCode, orderBy, ids, keyword, timeFrom, timeTo, current, size, offset, total, count, debug）
  - 提供 `copy()` / `copyIfNotNull()` 静态方法实现实体拷贝
- `Pageable`：分页接口，定义 `DEFAULT_CURRENT=1`, `DEFAULT_SIZE=10`，`init()` 默认方法校验并计算 offset
- `PageData<T>`：泛型分页封装，工厂方法 `fromEntity()` / `of()` / `empty()` / `convert()`
- `UserInfo`：登录用户信息（userCode, username, nickname, mobile[脱敏], tenantCode, avatar, openId）
- 所有业务实体必须继承 `BaseEntity`

### 异常体系

- `CommonException`：业务异常基类（继承 RuntimeException），持有 `code` 字段
- 构造器支持：`(String)`, `(ResultCode)`, `(int, String)`, `(String, Throwable)`, `(ResultCode, Throwable)`, `(int, String, Throwable)`
- 静态工厂方法：`of(String message, Object... args)` — 使用 `StringFormat.of()` 模板格式化
- 7 个子类（结构一致，均继承 CommonException）：
  - `ApiException` — API 调用异常
  - `ApplicationException` — 应用级业务异常
  - `NotFoundException` — 资源未找到
  - `SystemException` — 系统级异常
  - `UnauthorizedException` — 未授权
  - `UserException` — 用户操作异常
  - `ValidationException` — 数据校验异常

### 返回结果

- `R<T>`：统一响应结果类，字段：code, msg, data, requestTime, responseTime, costTime
- 使用方式：`R.ok(data)`, `R.warn(message)`, `R.warn(template, args)`, `R.error(message)`, `R.error(CommonException)`, `R.error(code, message)`, `R.error(template, args)`
- warn 对应 ResultCode.VALIDATION_ERROR(400)，error 对应 ResultCode.ERROR(500)

### 结果码

- `ResultCode`：枚举类，定义标准 HTTP 状态码和业务错误码
- HTTP 标准码：200/400/401/403/404/500
- 业务码段：10001-10102 Token/登录, 20001-20004 跨域/路由, 30001-30005 登录/验证码, 40001-40006 数据操作, 50001-50003 网络, 60001-60003 订单

### 用户上下文

- `UserContext`：基于 `ThreadLocal<UserInfo>` 的用户上下文
- `setUserInfo()` / `getUserInfo()` / `getUserCode()` / `getTenantCode()` / `clear()`

### 数据库操作

- `BaseMapper<T extends BaseEntity>`：通用 Mapper 接口，14 个方法，全部使用 `@XxxProvider` 注解动态生成 SQL
- `BaseService<T, M>`：通用 Service 抽象类，`@Service` + `@Transactional`，BATCH_SIZE=1000
- MyBatis 拦截器链：
  - `MyBatisUpdateInterceptor`：自动填充 createBy / updateBy，清空 createTime / updateTime 让数据库自动填充
  - `MyBatisQueryInterceptor`：空字符串替换为 null
  - `MyBatisBoundSqlInterceptor`：向 BoundSql 注入 updateBy 参数（用于非实体参数方法）
- 逻辑删除：deleted 字段，查询条件自动追加 `deleted = 0`
- 乐观锁：version 字段，更新时自动追加 `version = version + 1` 条件
- `@Blob` 标注的字段在 List 查询时不返回（selectListFields 排除）
- 排序安全：`buildOrderByClause()` 白名单校验防止 SQL 注入

### SPI 机制

- `UserNameProvider`：SPI 接口，默认方法 `getNamesByUserCodes(Set<String>)` 返回空 Map
- 由业务系统实现，供 `UserNameBodyAdvice` 自动填充 createByName / updateByName

## 开发与环境配置

### 构建命令

```bash
# 编译（跳过测试）
mvn clean compile -DskipTests

# 打包
mvn clean package -DskipTests

# 安装到本地仓库
mvn clean install -DskipTests

# 仅编译某个模块
mvn clean compile -pl sh-core -am -DskipTests
```

### 运行示例

```bash
# 启动 sh-demo（端口 8080，profile 默认 local）
cd sh-demo
mvn spring-boot:run
```

### sh-demo 默认配置

```yaml
server:
  port: 8080
spring:
  profiles:
    active: local
mybatis:
  configuration:
    map-underscore-to-camel-case: true
pagehelper:
  helper-dialect: mysql
```

### 环境要求

- JDK 25
- Maven 3.9+
- MySQL 8.0+
- Redis 6.0+（如使用 sh-redis）
- MQTT Broker（如使用 sh-mqtt）
- XXL-Job Admin（如使用 sh-xxljob）

> CI/CD 配置：当前项目未配置 CI/CD 流水线（无 .github/workflows、Dockerfile、docker-compose.yml 等）。

## 编码规范与最佳实践

### 命名规范

- 类名：PascalCase（如 `BaseEntity`, `RedisHelper`），Helper/Util 后缀表示工具类，Provider 后缀表示 SQL 生成器
- 方法名：camelCase（如 `selectByEntity`, `getUserCode`），静态工厂方法统一用 `of()`
- 常量：UPPER_SNAKE_CASE（如 `DEFAULT_SIZE`, `BATCH_SIZE`）
- 包名：全小写 `com.wkclz.{模块}`（如 `com.wkclz.core.base`）

### 注解使用

| 注解 | 模块 | 作用域 | 用途 |
|------|------|--------|------|
| `@Router(module, prefix)` | sh-core | TYPE | 路由标识，定义模块和前缀 |
| `@Blob` | sh-mybatis | FIELD | 标记 Blob 字段，List 查询不返回 |
| `@MqttController` | sh-mqtt | TYPE | MQTT 消息处理器（含 @Component） |
| `@MqttTopicMapping` | sh-mqtt | METHOD | 订阅子 Topic |
| `@AtLeastOneNotNull` | sh-web | TYPE | 类级校验，指定字段至少一个非空。**注意：被校验字段不应同时使用 @NotNull，否则会导致冲突** |

### 日志规范

- 使用 SLF4J + Logback，`@Slf4j` 注解（Lombok）
- 高频操作使用 `log.debug()`，避免 `log.info()` 产生大量日志
- 异常日志必须包含异常对象：`log.error("message: {}", arg, e)`

### 异常处理

- 业务异常继承 `CommonException`
- 不要吞没异常（空的 catch 块）
- 使用静态工厂方法创建异常：`SystemException.of("message: {}", arg)`，基于 `StringFormat.of()` 模板
- ErrorHandler 全局处理 8 种异常类型，UserException 只记 biz error 日志不发邮件，其他异常触发邮件告警

### 并发与安全

- DCL 双重检查（volatile + synchronized）用于懒加载单例
- ConcurrentHashMap 用于缓存（如 ENTITY_CACHE、FIELD_CACHE）
- CompletableFuture 用于异步创建（如动态数据源）
- RedisLock 使用 SETNX + Lua 原子释放 + Watchdog 自动续期
- 排序白名单校验防止 SQL 注入
- 敏感配置支持 RSA 密钥库 / AES 对称密钥 / 明文三种模式

## 关键工具类

| 类名 | 模块 | 用途 |
|------|------|------|
| `StringFormat` | sh-tool | 字符串模板格式化，支持 `{}` 占位符和 `${var}` 命名变量 |
| `StringUtil` | sh-tool | 字符串工具（下划线/驼峰转换等） |
| `BeanUtil` | sh-tool | Bean 拷贝（cpAll / cpNotNull / removeBlank） |
| `DateUtil` | sh-tool | 日期工具 |
| `JsonUtil` | sh-tool | JSON 工具（基于 fastjson2） |
| `MapUtil` | sh-tool | Map 工具（obj2Map） |
| `FileUtil` | sh-tool | 文件 IO 工具 |
| `SnowflakeIdWorker` | sh-tool | 雪花 ID 算法 |
| `NetworkUtil` | sh-tool | 网络工具（获取服务器 IP 等） |
| `SecretUtil` | sh-tool | 加密工具入口 |
| `QrCodeUtil` | sh-tool | 二维码生成 |
| `ValidateCode` | sh-tool | 验证码生成 |
| `JsUtil` | sh-tool | JS 脚本引擎 |
| `ClassUtil` | sh-tool | 类扫描工具 |
| `CompressUtil` | sh-tool | 压缩工具 |
| `CheckPwdUtil` | sh-tool | 密码强度检查 |
| `AesTool` / `DesTool` / `RsaTool` | sh-tool | 对称/非对称加密 |
| `Md5Tool` / `ShaTool` / `Base64Tool` | sh-tool | 摘要/编码 |
| `RedisHelper` | sh-redis | Redis 全数据类型操作（String/Hash/List/Set/ZSet） |
| `RedisLock` | sh-redis | 分布式锁（SETNX + Lua + Watchdog） |
| `RedisIdGenerator` | sh-redis | ID 生成器（时间戳 + 机器标识 + Redis 自增，Base62 编码） |
| `RedisMessageQueue` | sh-redis | Redis List 消息队列 |
| `IpHelper` | sh-web | IP 地址解析（x-forwarded-for 链路） |
| `RequestHelper` | sh-web | 请求工具（路径匹配、域名解析） |
| `ResponseHelper` | sh-web | 响应工具（错误写入、Excel 流式输出） |
| `RestHelper` | sh-web | REST 接口元数据扫描（提取参数信息、返回类型、泛型信息） |
| `LocalThreadHelper` | sh-web | 线程上下文（ThreadLocal<ConcurrentHashMap>） |
| `SpringContextHolder` | sh-spring | Spring 上下文全局持有器（静态获取 Bean） |
| `SnowflakeHelper` | sh-spring | 雪花 ID 辅助类（workId = 网卡 hashCode % 31） |
| `MailUtil` | sh-spring | 邮件发送（HTML / 内嵌图片 / 附件） |
| `PageQuery` | sh-mybatis | 分页查询工具（支持 BaseEntity 和 Pageable 接口） |
| `MqttProducer` | sh-mqtt | MQTT 消息发布（即时 / 延时 / 批量） |
| `PrincipalContext` | sh-iam-contract-api | Principal 读取上下文（基于 RequestContextHolder + ThreadLocal 双存储） |
| `AuthContract` | sh-iam-contract-api | 认证契约 SPI（authenticate + checkToken） |
| `AuthzContract` | sh-iam-contract-api | 鉴权契约 SPI（租户/应用/菜单/接口/字段/数据六维度，含上下文重载） |
| `AkSignContract` | sh-iam-contract-api | AK 签名契约 SPI（sign + verifySign） |
| `SsoFacadeContract` | sh-iam-contract-api | SSO 门面契约 SPI（login + saveLog + logout） |
| `ContractSettings` | sh-iam-contract-api | 静态配置持有器（供 default 方法访问） |
| `DefaultAuthFilter` | sh-iam-contract-default | 默认鉴权过滤器（调用 AuthContract SPI） |
| `IamContractAutoConfig` | sh-iam-contract-default | 自动配置（@ConditionalOnMissingBean 注册默认实现） |

## 配置属性前缀

所有框架配置统一使用 `sh` 前缀（MQTT 模块使用 `shrimp.cloud.mqtt`）：

| 前缀 | 模块 | 说明 |
|------|------|------|
| `sh.swagger.*` | sh-web | Swagger API 文档配置 |
| `sh.mqtt.*` | sh-mqtt | MQTT 配置（实际前缀为 `shrimp.cloud.mqtt.*`） |
| `sh.redis.*` | sh-redis | Redis 配置 |
| `sh.xxl-job.*` | sh-xxljob | XXL-Job 配置 |
| `sh.dynamicdb.cache-second` | sh-dynamicdb | 数据源缓存时间（秒，默认 60） |
| `sh.dynamicdb.cleanup-interval-second` | sh-dynamicdb | 清理间隔（秒，默认 120） |
| `sh.config.keystore.path` | sh-spring | RSA 密钥库路径 |
| `sh.config.keystore.alias` | sh-spring | 密钥库别名 |
| `sh.config.keystore.password` | sh-spring | 密钥库密码 |
| `sh.config.decrypt-aes-key` | sh-spring | AES 解密密钥 |
| `alarm.email.*` | sh-spring | 告警邮件配置 |
| `spring.application.name` | sh-spring | 应用名（XXL-Job appName 默认取此值） |
| `spring.profiles.active` | sh-spring | 环境标识（推断 EnvType） |
| `sh.iam.contract.enabled` | sh-iam-contract | 是否启用契约层自动配置（默认 true，@ConditionalOnProperty） |
| `iam.contract.auth-filter-enabled` | sh-iam-contract | 是否注册 DefaultAuthFilter（默认 true） |
| `iam.contract.public-path-pattern` | sh-iam-contract | 公开路径匹配模式（默认 `/*/public/**`） |
| `iam.contract.app-id` | sh-iam-contract | AK 签名 appId |
| `iam.contract.app-secret` | sh-iam-contract | AK 签名 appSecret（RSA 私钥） |
| `iam.contract.public-key` | sh-iam-contract | AK 验签 publicKey（RSA 公钥） |
| `iam.contract.server-url` | sh-iam-contract | SSO 服务端地址 |
| `iam.contract.jwt-secret-key` | sh-iam-contract | JWT 密钥（供实现层使用） |

## AI 代理协作指南

### 修改代码时的注意事项

1. **依赖方向不可逆**：sh-tool → sh-core → sh-mybatis/sh-spring → 上层模块。**禁止反向依赖**（如 sh-tool 不能引用 sh-core 的类）。
2. **新增模块**：需在根 `pom.xml` 的 `<modules>` 中注册，在 sh-parent 的 `<dependencyManagement>` 中添加版本，并在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册自动配置类。
3. **依赖版本管理**：所有三方依赖版本统一在 `sh-bom` 中管理，子模块不得硬编码版本号。新增依赖先在 sh-bom 的 `<dependencyManagement>` 中声明版本属性。
4. **实体规范**：业务实体必须继承 `BaseEntity`，以获得分页、查询、逻辑删除、乐观锁、审计字段自动填充等能力。
5. **异常规范**：使用 `XxxException.of("message: {}", arg)` 静态工厂方法，不要 `new XxxException(String.format(...))`。
6. **消息格式化**：使用 `StringFormat.of()` 格式化异常消息，不要使用字符串拼接。
7. **敏感配置**：密码、密钥不要硬编码，使用配置注入或环境变量，敏感值使用 `ENC(...)` 包裹并配置解密方式。
8. **MyBatis 拦截器链**：修改拦截器时注意执行顺序（Query → Update → BoundSql），以及 `@Blob` / `deleted` / `version` 的自动处理逻辑。
9. **Redis 操作容错**：`RedisHelper` 所有方法 try-catch 返回默认值（false/null/0），不向上抛出。如果业务需要感知 Redis 异常，需自行处理。
10. **MQTT 配置前缀**：MQTT 使用 `shrimp.cloud.mqtt.*` 而非 `sh.mqtt.*`，修改配置时注意区分。
11. **自动配置命名**：大部分模块使用 `Sh{Module}AutoConfig`，但 sh-xxljob 使用 `XxlJobAutoConfigure`，sh-mqtt 使用 `MqttAutoConfigure`，新增模块建议统一为 `Sh{Module}AutoConfig`。

### 避免破坏的现有模式

- **BaseMapper 的 14 个方法**：所有 SQL 由 Provider 动态生成，不要在 BaseMapper 中添加 XML 映射。
- **拦截器自动填充**：createBy / updateBy 由 `MyBatisUpdateInterceptor` 从 `UserContext` 自动设置，不要在业务代码中手动赋值。
- **PageHelper 分页**：`BaseService.selectPage()` 内部使用 `selectCountByEntity` + `selectByEntityWithLimit` 实现分页，不要在 Service 层再调用 PageHelper.startPage()。
- **逻辑删除**：`deleted` 字段由 `buildWhereClause()` 自动追加 `deleted = 0`，不要在业务 SQL 中手动添加此条件。
- **UserNameBodyAdvice**：响应体自动填充 createByName / updateByName，通过 SPI 接口 `UserNameProvider` 实现，不要在 Service 层手动查询用户名。
- **DynamicDataSourceAop**：Mapper 方法执行后自动清理 ThreadLocal，不要手动调用 `DynamicDataSourceHolder.clear()`。

### 推荐的测试策略

- 单元测试使用 Spring Boot Test（`spring-boot-starter-test`），sh-core 已引入。
- 测试 MyBatis 模块时需配置内存数据库（H2）或使用 Testcontainers（MySQL）。
- 测试 Redis 模块建议使用 Testcontainers（Redis）或嵌入式 Redis。
- 测试 MQTT 模块建议使用 MQTT Broker 的测试容器。
- sh-demo 模块可作为集成测试的参考。

### 已知问题

- sh-mqtt 的 `org.eclipse.paho.client.mqttv3` 版本 `1.2.5` 直接写在 pom.xml 中，未使用 sh-bom 中定义的 `${mqttv3.version}` 属性。
- sh-mybatis 包含修改版 `PageInterceptor`（在 `com.github.pagehelper` 包下），覆盖了原始 PageHelper 的拦截器。
- sh-web 的 mysql-connector-j 和 spring-jdbc 标记为 `<optional>true</optional>`，仅用于 ErrorHandler 获取数据库异常信息。
- sh-mybatis 的 `DeleteByIdMapperProvider` 和 `DeleteByIdsMapperProvider` 中 `#{updateBy}` 必须显式声明 `javaType=String`（即 `#{updateBy, javaType=String}`），否则 MyBatis 会从方法参数类型 `Long` 错误推断 javaType，导致 `MyBatisBoundSqlInterceptor` 注入 String 值时抛出 ClassCastException。

## 用户故事索引

所有用户故事文档位于 `/docs/stories/` 目录下，按模块分组。每个故事包含用户故事描述、验收标准（含异常场景）和涉及代码上下文。共 31 个用户故事。

### sh-core（6 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-001 | [US-001-实体体系与数据规范.md](docs/stories/US-001-实体体系与数据规范.md) | 实体体系与数据规范 | 高 |
| US-002 | [US-002-统一响应结果封装.md](docs/stories/US-002-统一响应结果封装.md) | 统一响应结果封装 | 高 |
| US-003 | [US-003-异常体系与分类处理.md](docs/stories/US-003-异常体系与分类处理.md) | 异常体系与分类处理 | 高 |
| US-004 | [US-004-用户上下文与多租户隔离.md](docs/stories/US-004-用户上下文与多租户隔离.md) | 用户上下文与多租户隔离 | 高 |
| US-005 | [US-005-结果码与业务错误码体系.md](docs/stories/US-005-结果码与业务错误码体系.md) | 结果码与业务错误码体系 | 中 |
| US-006 | [US-006-日志脱敏与安全输出.md](docs/stories/US-006-日志脱敏与安全输出.md) | 日志脱敏与安全输出 | 中 |

### sh-mybatis（5 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-007 | [US-007-通用Mapper与动态SQL生成.md](docs/stories/US-007-通用Mapper与动态SQL生成.md) | 通用 Mapper 与动态 SQL 生成 | 高 |
| US-008 | [US-008-逻辑删除与数据安全.md](docs/stories/US-008-逻辑删除与数据安全.md) | 逻辑删除与数据安全 | 高 |
| US-009 | [US-009-乐观锁与并发控制.md](docs/stories/US-009-乐观锁与并发控制.md) | 乐观锁与并发控制 | 高 |
| US-010 | [US-010-MyBatis拦截器与自动填充.md](docs/stories/US-010-MyBatis拦截器与自动填充.md) | MyBatis 拦截器与自动填充 | 高 |
| US-011 | [US-011-分页查询与PageData封装.md](docs/stories/US-011-分页查询与PageData封装.md) | 分页查询与 PageData 封装 | 高 |

### sh-web（4 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-012 | [US-012-全局异常处理与邮件告警.md](docs/stories/US-012-全局异常处理与邮件告警.md) | 全局异常处理与邮件告警 | 高 |
| US-013 | [US-013-响应体用户名自动填充.md](docs/stories/US-013-响应体用户名自动填充.md) | 响应体用户名自动填充 | 中 |
| US-014 | [US-014-REST接口元数据扫描.md](docs/stories/US-014-REST接口元数据扫描.md) | REST 接口元数据扫描 | 中 |
| US-015 | [US-015-自定义参数校验与标准请求Bean.md](docs/stories/US-015-自定义参数校验与标准请求Bean.md) | 自定义参数校验与标准请求 Bean | 中 |

### sh-redis（4 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-016 | [US-016-Redis全数据类型缓存操作.md](docs/stories/US-016-Redis全数据类型缓存操作.md) | Redis 全数据类型缓存操作 | 高 |
| US-017 | [US-017-Redis分布式锁.md](docs/stories/US-017-Redis分布式锁.md) | Redis 分布式锁 | 高 |
| US-018 | [US-018-Redis-ID生成器.md](docs/stories/US-018-Redis-ID生成器.md) | Redis ID 生成器 | 中 |
| US-019 | [US-019-Redis消息队列.md](docs/stories/US-019-Redis消息队列.md) | Redis 消息队列 | 中 |

### sh-dynamicdb（2 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-020 | [US-020-动态数据源运行时切换.md](docs/stories/US-020-动态数据源运行时切换.md) | 动态数据源运行时切换 | 高 |
| US-021 | [US-021-动态数据源DCL与异步创建.md](docs/stories/US-021-动态数据源DCL与异步创建.md) | 动态数据源 DCL 与异步创建 | 中 |

### sh-spring（2 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-022 | [US-022-Spring上下文全局持有器.md](docs/stories/US-022-Spring上下文全局持有器.md) | Spring 上下文全局持有器 | 高 |
| US-023 | [US-023-雪花ID与系统初始化.md](docs/stories/US-023-雪花ID与系统初始化.md) | 雪花 ID 与系统初始化 | 中 |

### sh-mqtt（2 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-024 | [US-024-MQTT注解驱动消息发布订阅.md](docs/stories/US-024-MQTT注解驱动消息发布订阅.md) | MQTT 注解驱动消息发布/订阅 | 高 |
| US-025 | [US-025-MQTT-SSL-TLS认证与断线重连.md](docs/stories/US-025-MQTT-SSL-TLS认证与断线重连.md) | MQTT SSL/TLS 认证与断线重连 | 中 |

### sh-xxljob（1 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-026 | [US-026-XXL-Job定时任务集成.md](docs/stories/US-026-XXL-Job定时任务集成.md) | XXL-Job 定时任务集成 | 中 |

### sh-tool（3 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-027 | [US-027-加密工具集.md](docs/stories/US-027-加密工具集.md) | 加密工具集（AES/DES/RSA/MD5/SHA/Base64） | 高 |
| US-028 | [US-028-字符串格式化与Bean操作工具.md](docs/stories/US-028-字符串格式化与Bean操作工具.md) | 字符串格式化与 Bean 操作工具 | 高 |
| US-029 | [US-029-综合工具集.md](docs/stories/US-029-综合工具集.md) | 综合工具集（日期/文件/网络/验证码/二维码/JS引擎） | 中 |

### sh-demo（1 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-030 | [US-030-示例模块CRUD标准范式.md](docs/stories/US-030-示例模块CRUD标准范式.md) | 示例模块 CRUD 标准范式 | 高 |

### sh-iam-contract（1 个故事）

| 故事ID | 文档 | 标题 | 优先级 |
|--------|------|------|--------|
| US-031 | [US-031-IAM契约层.md](docs/stories/US-031-IAM契约层.md) | IAM 契约层（认证/鉴权/AK 签名/SSO 门面） | 高 |

## 开发注意事项

1. 新增模块需在根 `pom.xml` 的 `<modules>` 中注册
2. 新增自动配置需在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册
3. 依赖版本统一在 `sh-bom` 中管理，不要在子模块中硬编码版本号
4. 业务实体必须继承 `BaseEntity`，以获得分页、查询、逻辑删除等能力
5. 使用 `StringFormat.of()` 格式化异常消息，不要使用字符串拼接
6. 敏感配置（密码、密钥）不要硬编码，使用配置注入或环境变量
7. 修改拦截器时注意执行顺序和自动处理逻辑（@Blob / deleted / version）
8. Redis 操作默认容错（不抛异常），如需感知异常需自行处理

## 编码规则

> 以下规则为 harness 工程强制规范，AI 编码时必须遵循：

1. **禁止调用系统资源**：仅能使用当前目录下的代码资源，不得调用系统级命令或外部系统资源
2. **保留已有注释**：不要移除已添加的注释，除非相关代码块已变动
3. **关键位置加日志**：实现业务逻辑时，在关键位置添加 log 日志打印（方法入口、分支判断、异常捕获、外部调用）
4. **更新文档**：任务完成后，必须更新本文件（AGENTS.md）以及相关的故事文件
5. **Req/Resp 封装**：所有请求参数封装 Req 对象（除非参数只有一个值），所有返回内容封装 Resp 对象（除非返回只有一个值）
