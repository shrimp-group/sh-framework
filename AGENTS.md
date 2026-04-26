# AGENTS.md

## 项目概述

sh-framework 是一个基于 Spring Boot 4.0 的 Java 后端基础框架，使用 Java 25 编译。项目采用 Maven 多模块结构，提供通用的基础设施能力，包括 ORM、缓存、MQ、动态数据源、定时任务等。

- **GroupId**: `com.wkclz.framework`
- **版本管理**: 使用 `${revision}` 占位符 + flatten-maven-plugin 管理版本
- **当前版本**: `5.0.0-SNAPSHOT`
- **基础包名**: `com.wkclz`

## 模块结构

```
sh-framework/
├── sh-parent      # 父 POM，统一依赖管理
├── sh-bom         # BOM（物料清单），管理三方依赖版本
├── sh-tool        # 工具类模块（加密、字符串、日期、文件等）
├── sh-core        # 核心模块（基础实体、异常、枚举、用户上下文）
├── sh-mybatis     # MyBatis 模块（BaseMapper、BaseService、拦截器）
├── sh-spring      # Spring 扩展模块（上下文、配置、雪花ID、邮件）
├── sh-dynamicdb   # 动态数据源模块（运行时切换数据源）
├── sh-redis       # Redis 模块（缓存、分布式锁、消息队列）
├── sh-web         # Web 模块（全局异常处理、IP、请求/响应工具）
├── sh-xxljob      # XXL-Job 定时任务模块
├── sh-mqtt        # MQTT 消息模块（发布/订阅）
└── sh-demo        # 示例模块（演示如何使用框架）
```

### 模块依赖关系

```
sh-tool（无依赖）
  ↑
sh-core
  ↑
sh-mybatis
  ↑
sh-spring
  ↑
sh-dynamicdb / sh-redis / sh-web / sh-xxljob / sh-mqtt
```

## 核心约定

### 实体体系

- `DbColumnEntity`：数据库规范字段基类（id, sort, createTime, createBy, updateTime, updateBy, remark, version）
- `BaseEntity extends DbColumnEntity`：业务实体基类，增加分页、查询辅助字段（userCode, tenantCode, orderBy, ids, keyword, timeFrom, timeTo, current, size, offset, total, count, debug）
- 所有业务实体必须继承 `BaseEntity`

### 异常体系

- `CommonException`：业务异常基类，所有业务异常必须继承此类
- 子类：`ApiException`, `ApplicationException`, `NotFoundException`, `SystemException`, `UnauthorizedException`, `UserException`, `ValidationException`
- 推荐使用静态工厂方法创建异常：`SystemException.of("message: {}", arg)`

### 返回结果

- `R<T>`：统一响应结果类，包含 code, msg, data, requestTime, responseTime, costTime
- 使用方式：`R.ok(data)`, `R.error(message)`, `R.warn(message)`

### 结果码

- `ResultCode`：枚举类，定义标准 HTTP 状态码和业务错误码
- 200: 成功, 400: 参数校验失败, 401: 未授权, 500: 服务器错误
- 业务码段：10001-10102 Token/登录, 20001-20004 跨域/路由, 30001-30004 登录/验证码, 40001-40006 数据操作, 50001-50003 网络, 60001-60003 订单

### 用户上下文

- `UserContext`：基于 ThreadLocal 的用户上下文，存储登录用户信息
- 通过 `UserContext.getUserCode()` 获取当前用户编码
- 通过 `UserContext.getTenantCode()` 获取当前租户编码

### 数据库操作

- `BaseMapper<T extends BaseEntity>`：通用 Mapper 接口，提供单表 CRUD
- `BaseService<T, M>`：通用 Service 抽象类，提供分页查询等能力
- MyBatis 拦截器自动填充 createBy/updateBy（`MyBatisUpdateInterceptor`）
- 逻辑删除：deleted 字段，查询条件自动追加 `deleted = 0`

### 自动配置

- 每个模块通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册自动配置
- 自动配置类命名规范：`Sh{Module}AutoConfig`

## 代码规范

### 命名规范

- 类名：PascalCase（如 `BaseEntity`, `RedisHelper`）
- 方法名：camelCase（如 `selectByEntity`, `getUserCode`）
- 常量：UPPER_SNAKE_CASE（如 `DEFAULT_SIZE`, `BATCH_SIZE`）
- 包名：全小写（如 `com.wkclz.core.base`）

### 注解使用

- `@FieldDesc("描述")`：自定义注解，用于标注字段含义
- `@Router`：路由注解
- `@Blob`：标记 Blob 类型字段
- `@MqttController` + `@MqttTopicMapping`：MQTT 消息处理

### 日志规范

- 使用 SLF4J + Logback
- 使用 `@Slf4j` 注解（Lombok）
- 高频操作使用 `log.debug()`，避免 `log.info()` 产生大量日志
- 异常日志必须包含异常对象：`log.error("message: {}", arg, e)`

### 异常处理

- 业务异常继承 `CommonException`
- 不要吞没异常（空的 catch 块）
- 使用 `SystemException.of()` 创建系统异常，支持模板参数

## 关键工具类

| 类名 | 模块 | 用途 |
|------|------|------|
| `StringFormat` | sh-tool | 字符串格式化，支持 `{}` 占位符和 `${var}` 命名变量 |
| `StringUtil` | sh-tool | 字符串工具（下划线/驼峰转换等） |
| `DateUtil` | sh-tool | 日期工具 |
| `JsonUtil` | sh-tool | JSON 工具（基于 fastjson2） |
| `MapUtil` | sh-tool | Map 工具 |
| `BeanUtil` | sh-tool | Bean 拷贝工具 |
| `SnowflakeIdWorker` | sh-tool | 雪花 ID 生成器 |
| `NetworkUtil` | sh-tool | 网络工具（获取服务器 IP 等） |
| `RedisHelper` | sh-redis | Redis 操作工具（String/Hash/List/Set/ZSet） |
| `RedisLock` | sh-redis | 分布式锁 |
| `IpHelper` | sh-web | IP 地址工具 |
| `SpringContextHolder` | sh-spring | Spring 上下文工具 |
| `SnowflakeHelper` | sh-spring | 雪花 ID 辅助类 |

## 配置前缀

所有框架配置统一使用 `shrimp.cloud` 前缀：

- `shrimp.cloud.mqtt.*`：MQTT 配置
- `shrimp.cloud.redis.*`：Redis 配置
- `shrimp.cloud.xxl-job.*`：XXL-Job 配置

## 技术栈

- **Java**: 25
- **Spring Boot**: 4.0.0
- **ORM**: MyBatis + PageHelper
- **连接池**: Druid
- **数据库**: MySQL
- **缓存**: Redis (Lettuce)
- **消息队列**: MQTT (Eclipse Paho)
- **定时任务**: XXL-Job
- **JSON**: fastjson2
- **工具库**: Hutool, Guava, Apache Commons
- **加密**: BouncyCastle
- **构建**: Maven + flatten-maven-plugin

## 开发注意事项

1. 新增模块需在根 `pom.xml` 的 `<modules>` 中注册
2. 新增自动配置需在 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中注册
3. 依赖版本统一在 `sh-bom` 中管理，不要在子模块中硬编码版本号
4. 业务实体必须继承 `BaseEntity`，以获得分页、查询、逻辑删除等能力
5. 使用 `StringFormat.of()` 格式化异常消息，不要使用字符串拼接
6. 敏感配置（密码、密钥）不要硬编码，使用配置注入或环境变量
