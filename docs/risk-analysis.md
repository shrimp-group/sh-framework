# sh-framework 深度风险评估报告

> **评估日期**：2026-06-02
> **评估范围**：sh-framework 全部 10 个模块（sh-tool / sh-core / sh-mybatis / sh-spring / sh-dynamicdb / sh-redis / sh-web / sh-mqtt / sh-xxljob / sh-demo）
> **评估方法**：基于源代码静态审计，结合架构设计模式与运行时行为推断

---

## 一、性能风险

### 1.1 [高] MyBatis SQL Provider 反射开销与缓存缺失

**位置**：`sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/BaseMapperProvider.java`

**问题**：14 个 SQL Provider 每次执行时均通过 `getDbEntityProperty()` 获取实体元数据。虽然 `DbEntityProperty` 使用了 `ConcurrentHashMap` 缓存，但 `getFieldValue()` 方法每次调用都通过反射 `Field.get()` 获取字段值，在高并发写入场景下反射调用开销不可忽视。

**后果**：高并发插入/更新时，反射调用成为 CPU 热点，QPS 下降。

**建议**：在 `DbEntityProperty` 中预编译 `MethodHandle` 或缓存 `Getter/Setter` 的 `Method` 对象，替代每次 `Field.get()` 调用。`BeanUtil.getJavaField()` 已缓存了 getter/setter Method，但 SQL Provider 未复用。

---

### 1.2 [高] 动态数据源 DCL 同步锁阻塞

**位置**：`sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/DynamicDataSource.java` — `determineCurrentLookupKey()`

**问题**：当数据源缓存过期时，`synchronized(this)` 会阻塞所有需要路由的数据库请求。`CompletableFuture.get()` 是阻塞调用，在异步线程创建数据源期间，当前线程被阻塞，同时锁被持有，其他线程也无法通过 DCL 检查。

**后果**：多租户场景下，一个租户数据源过期重建时，所有租户的数据库请求被串行化，造成严重性能抖动。

**建议**：
- 使用 `ConcurrentHashMap.computeIfAbsent()` 替代 `synchronized + DCL`，锁粒度从全局降到 key 级别。
- 将 `CompletableFuture.get()` 改为非阻塞模式，缓存未命中时先返回默认数据源，异步创建完成后下次请求自动路由。

---

### 1.3 [中] RedisHelper 写操作吞异常导致静默失败

**位置**：`sh-redis/src/main/java/com/wkclz/redis/helper/RedisHelper.java`

**问题**：所有写操作（`set`/`hSet`/`lPush`/`sAdd`/`zAdd` 等）均使用 try-catch 包裹，异常时返回 `false`/`0`/`null` 而不抛出。调用方若未检查返回值，写操作静默失败，数据丢失而无感知。

**后果**：缓存写入失败但业务流程继续执行，导致缓存与数据库数据不一致，且问题难以追踪。

**建议**：
- 提供两套 API：`set()` 静默失败版和 `setOrThrow()` 抛异常版，让调用方按场景选择。
- 至少在 `log.warn()` 中记录失败信息，便于排查。

---

### 1.4 [中] UserNameBodyAdvice 递归反射性能开销

**位置**：`sh-web/src/main/java/com/wkclz/web/rest/UserNameBodyAdvice.java`

**问题**：每个 REST 响应写出前，`UserNameBodyAdvice` 都会递归遍历响应体收集 `BaseEntity` 实例。反射遍历对象字段（最深 8 层）+ `FIELD_CACHE` 查找，在大数据量分页查询（如 1000 条记录）时，性能开销显著。

**后果**：分页查询接口响应时间增加，数据量越大影响越明显。

**建议**：
- 增加开关配置，允许按接口粒度禁用自动填充。
- 对 `R<PageData<T>>` 结构做短路优化，直接从 `records` 列表提取，避免递归。
- 考虑异步填充用户名，不阻塞响应写出。

---

### 1.5 [低] PageHelper 与手动 LIMIT 分页并存

**位置**：`sh-mybatis/src/main/java/com/wkclz/mybatis/service/BaseService.java` 与 `sh-mybatis/src/main/java/com/wkclz/mybatis/helper/PageQuery.java`

**问题**：框架同时提供两种分页策略——`BaseService.selectPage()` 使用手动 `LIMIT offset, size`，`PageQuery.page()` 使用 PageHelper 拦截。若在同一请求中混用，PageHelper 的 `startPage` 可能影响手动 LIMIT 查询，导致分页结果错乱。

**后果**：分页数据不正确，且难以定位是哪种分页策略导致的问题。

**建议**：统一为一种分页策略，推荐使用 PageHelper（更成熟），废弃手动 LIMIT 方式，并在文档中明确说明。

---

## 二、内存隐患

### 2.1 [高] ThreadLocal 泄漏风险

**位置**：多处使用 ThreadLocal

| 位置 | ThreadLocal | 清理时机 |
|------|------------|---------|
| `UserContext` | `USER_CONTEXT` | 依赖调用方手动 `clear()` |
| `DynamicDataSourceHolder` | `DATA_SOURCE_HOLDER` | AOP 在 Mapper 方法后清理 |
| `LocalThreadHelper` | `contextHolder` | 依赖 Filter 清理 |

**问题**：
- `UserContext.clear()` 没有自动清理机制，若业务代码在拦截器/过滤器之外设置 UserInfo（如 `@Async` 线程、`CompletableFuture` 线程），ThreadLocal 永远不会被清理。
- `LocalThreadHelper` 使用 `ThreadLocal<ConcurrentHashMap>`，如果 Filter 未调用 `clear()`，Map 中所有对象都不会被 GC。
- 线程池复用场景下，上一个请求的 ThreadLocal 残留会被下一个请求继承，导致用户上下文错乱。

**后果**：内存泄漏（线程池线程不销毁，ThreadLocal 持有对象无法 GC）+ 数据错乱（用户 A 看到用户 B 的数据）。

**建议**：
- 所有 ThreadLocal 统一在 Filter/Interceptor 的 `finally` 块中清理，不依赖业务代码。
- 使用 `InheritableThreadLocal` 的替代方案（如 `TransmittableThreadLocal`）处理线程池传递场景。
- 为 `UserContext` 增加 Servlet Filter 自动清理。

---

### 2.2 [高] DynamicDataSource 连接池未关闭导致资源泄漏

**位置**：`sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/DynamicDataSource.java`

**问题**：`destroyDataSource(key)` 方法需要手动调用，但框架没有提供数据源销毁的触发机制。当租户退租或数据源配置变更时，旧的 `DruidDataSource` 连接池不会被自动关闭。`hasCreateDataSource` Map 持有所有已创建数据源的引用，也不会清理。

**后果**：长时间运行后，废弃数据源的连接池持续占用数据库连接和内存，最终导致连接数耗尽或 OOM。

**建议**：
- 在 `DynamicDataSource` 中增加定时清理任务，扫描 `hasCreateDataSource` 中过期的数据源并关闭。
- 实现 `DisposableBean`，在应用关闭时关闭所有数据源。
- 增加 JMX 暴露，监控活跃数据源数量。

---

### 2.3 [中] RedisMessageQueueManager 线程池无界队列

**位置**：`sh-redis/src/main/java/com/wkclz/redis/queue/RedisMessageQueueManager.java`

**问题**：消费线程池使用 `LinkedBlockingQueue<>(1024)` 有界队列 + `CallerRunsPolicy` 拒绝策略。当消费速度跟不上生产速度时，`CallerRunsPolicy` 会让发送线程执行消费逻辑，如果消费逻辑阻塞，则发送线程也被阻塞。

**后果**：消息积压时，生产者线程被阻塞，导致上游请求超时。

**建议**：
- 使用自定义拒绝策略，记录告警日志而非阻塞生产者。
- 增加队列深度监控指标。
- 考虑使用 `SynchronousQueue` + 动态扩缩容线程池。

---

### 2.4 [中] MqttHandlerFactory 静态 Map 无清理机制

**位置**：`sh-mqtt/src/main/java/com/wkclz/mqtt/handler/MqttHandlerFactory.java`

**问题**：`mqttControllers`、`mqttHandlers`、`parentTopicSet` 三个静态 `ConcurrentHashMap` 在应用运行期间只增不减。虽然 Topic 数量通常有限，但如果存在动态注册/注销场景（如热部署），这些 Map 会导致 ClassLoader 泄漏。

**后果**：热部署场景下，旧的 Controller Bean 实例被 Map 持有无法 GC，导致 Metaspace / PermGen 泄漏。

**建议**：提供 `unregister()` 方法，支持 Topic 的动态注销。

---

### 2.5 [低] BeanUtil 反射缓存无过期策略

**位置**：`sh-tool/src/main/java/com/wkclz/tool/utils/BeanUtil.java`

**问题**：`CLASS_METHOD_CACHE` 和 `PROPERTY_DESCRIPTORS` 两个静态 ConcurrentHashMap 缓存了所有反射元数据，永不过期。若应用动态加载/卸载 Class（如插件化架构），缓存持有 Class 引用导致 ClassLoader 泄漏。

**后果**：插件化场景下 Metaspace 泄漏。

**建议**：使用 `WeakHashMap` 或 Caffeine 缓存（weakKeys 策略），当 Class 被卸载时缓存自动失效。

---

## 三、线程与并发风险

### 3.1 [高] DynamicDataSource 异步创建的死循环边界

**位置**：`sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/DynamicDataSource.java`

**问题**：`CompletableFuture.supplyAsync()` 使用 `ForkJoinPool.commonPool()` 默认线程池。如果 commonPool 线程耗尽（如被其他 CompletableFuture 任务占满），异步创建任务排队等待，而主线程在 `future.get()` 阻塞等待，形成线程饥饿死锁。

**后果**：极端情况下，所有 commonPool 线程被阻塞，动态数据源创建请求无法完成，所有数据库操作超时。

**建议**：使用独立的 `ExecutorService`（而非 commonPool）执行数据源创建任务，隔离资源。

---

### 3.2 [高] RedisLock 锁过期与业务执行时间不匹配

**位置**：`sh-redis/src/main/java/com/wkclz/redis/helper/RedisLock.java`

**问题**：`tryLock(key, lockTime, timeUnit)` 设置的过期时间是固定的。如果业务执行时间超过锁的过期时间，锁自动释放后其他线程获取锁，导致并发执行。原持有者执行完毕后调用 `releaseLock()`，由于 Lua 脚本校验 requestId，不会误删新锁，但业务层面的并发已经发生。

**后果**：分布式锁语义被破坏，本应互斥的操作并发执行，导致数据不一致。

**建议**：
- 实现锁续期机制（Watchdog），在业务执行期间定期延长锁的过期时间。
- 在业务层面增加幂等性保护，即使并发执行也不会产生脏数据。

---

### 3.3 [中] SnowflakeIdWorker 时钟回拨处理粗暴

**位置**：`sh-tool/src/main/java/com/wkclz/tool/utils/SnowflakeIdWorker.java`

**问题**：`nextId()` 方法检测到时钟回拨时直接抛出 `RuntimeException`，没有等待或容忍机制。在 NTP 时间同步场景下，小幅时钟回拨（几毫秒）是常见现象。

**后果**：NTP 同步导致 ID 生成服务短暂不可用，影响依赖 ID 生成的业务流程。

**建议**：
- 小幅回拨（< 5ms）时自旋等待时钟追平。
- 大幅回拨时记录告警并使用备用 workerId 生成 ID。
- 参考 `RedisIdGenerator` 的做法，回拨时使用上次时间戳。

---

### 3.4 [中] FreeMarkerTemplateUtil 全局 Configuration 非线程安全操作

**位置**：`sh-spring/src/main/java/com/wkclz/spring/utils/FreeMarkerTemplateUtil.java`

**问题**：`getTemplate()` 方法使用 `ReentrantLock` 保护 `setTemplateLoader()` / `setDirectoryForTemplateLoading()` 操作，但 `CONFIGURATION.getTemplate()` 在锁外执行。FreeMarker 的 `Configuration.getTemplate()` 本身是线程安全的，但 `setTemplateLoader()` 修改了共享状态，如果并发调用 `getTemplate(name)` 和 `getTemplate(name, dir)`，后者修改 TemplateLoader 后，前者可能读到不一致的配置。

**后果**：并发加载模板时可能返回错误的模板内容或抛出异常。

**建议**：为不同模板加载路径使用独立的 `Configuration` 实例，避免共享可变状态。

---

### 3.5 [低] RedisIdGenerator lastTimestamp/lastSequence 非原子操作

**位置**：`sh-redis/src/main/java/com/wkclz/redis/helper/RedisIdGenerator.java`

**问题**：`lastTimestamp` 和 `lastSequence` 使用 `volatile` 修饰，但两者的读写不是原子操作。在并发场景下，线程 A 读取 `lastTimestamp` 后，线程 B 可能修改了 `lastSequence`，导致 A 使用了过时的序列号。

**后果**：极低概率生成重复 ID（Redis increment 保证了 Redis 层面的唯一性，但本地降级模式下可能重复）。

**建议**：本地降级模式下使用 `synchronized` 保护 `lastTimestamp` 和 `lastSequence` 的读写，与 `SnowflakeIdWorker` 一致。

---

## 四、安全风险

### 4.1 [高] SQL 注入：deleteById/updateBy 字符串拼接

**位置**：`sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/DeleteByIdMapperProvider.java` 和 `DeleteByIdsMapperProvider.java`

**问题**：逻辑删除 SQL 中，`updateBy` 字段的值通过 `escapeSql()` 转义后直接拼接到 SQL 字符串中：

```java
sql.append(" AND update_by = '").append(escapeSql(userCode)).append("'");
```

虽然 `escapeSql()` 将单引号替换为双单引号，但这种防御方式不够健壮：
- 仅处理了单引号，未处理反斜杠、注释符 `--`、`/*` 等。
- `UserContext.getUserCode()` 的值来源不可控，若上游未做校验，可能包含恶意字符。

**后果**：潜在的 SQL 注入风险，攻击者可构造恶意 userCode 绕过认证或篡改数据。

**建议**：使用 `#{updateBy}` 参数绑定替代字符串拼接，与 `DeleteByIdEntityMapperProvider` 保持一致（后者已使用参数绑定）。

---

### 4.2 [高] fastjson2 AutoType 反序列化漏洞

**位置**：`sh-redis/src/main/java/com/wkclz/redis/serializer/Fastjson2JsonRedisSerializer.java`

**问题**：反序列化时启用了 `JSONReader.Feature.SupportAutoType`，允许根据 JSON 中的 `@type` 字段自动实例化任意类。这是 fastjson 历史上最严重的安全漏洞来源，即使 fastjson2 已做了安全加固，仍存在被绕过的风险。

**后果**：攻击者可构造恶意 JSON 数据（如通过 Redis 注入），实例化危险类（如 `java.lang.Runtime`）执行任意命令。

**建议**：
- 禁用 `SupportAutoType`，改为显式指定反序列化类型。
- 若必须使用 AutoType，配置 `safeMode` + 白名单 `autoTypeFilter`，仅允许业务类。

---

### 4.3 [高] 敏感配置明文存储

**位置**：`sh-spring/src/main/java/com/wkclz/spring/config/SystemConfig.java`

**问题**：`alarmEmailPassword`、`configDecryptAesKey` 等敏感配置通过 `@Value` 注入，但未要求加密存储。`application.yml` 中可能以明文形式存储密码和密钥。

**后果**：配置文件泄露时，攻击者可直接获取邮件密码、AES 密钥等敏感信息。

**建议**：
- 使用 Jasypt 或 Spring Cloud Config 加密存储敏感配置。
- `SystemConfig` 中增加解密逻辑，`@Value` 注入密文，运行时解密。
- 生产环境使用 Vault 或 K8s Secrets 管理密钥。

---

### 4.4 [中] RemoveReq 注解语义冲突

**位置**：`sh-web/src/main/java/com/wkclz/web/bean/RemoveReq.java`

**问题**：类级别 `@AtLeastOneNotNull(fields={"id", "ids"}, message="id 或 ids 必须填写其中一个")` 允许部分为 null，但字段级别 `@NotNull(message="主键ID不能为空")` 要求每个字段都不能为 null。两者语义冲突，实际运行时 `@NotNull` 先于 `@AtLeastOneNotNull` 生效，导致 `id` 和 `ids` 都不能为 null，与 `@AtLeastOneNotNull` 的设计意图矛盾。

**后果**：删除操作必须同时提供 `id` 和 `ids`，无法实现"二选一"的预期行为。

**建议**：移除 `RemoveReq` 字段上的 `@NotNull` 注解，仅保留类级别的 `@AtLeastOneNotNull`。

---

### 4.5 [中] 日志脱敏仅替换为星号，未保留部分信息

**位置**：`sh-core/src/main/java/com/wkclz/core/log/MaskingPatternLayout.java`

**问题**：`maskMessage()` 方法将匹配到的所有字符替换为 `*`，不保留部分信息。例如手机号 `13812345678` 被替换为 `***********`，而非 `138****5678`，不利于日志排查问题。

**后果**：日志完全脱敏后可读性差，运维排查问题时无法定位具体用户/记录。

**建议**：实现分组保留脱敏（如保留前 3 后 4 位），修改正则匹配策略，使用分组捕获。

---

### 4.6 [中] MailUtil 密码 toString 脱敏不彻底

**位置**：`sh-spring/src/main/java/com/wkclz/spring/utils/MailUtil.java`

**问题**：`toString()` 方法将 `emailPassword` 显示为 `******`，但 `emailHost`、`emailFrom`、`toEmails` 等信息仍然暴露。如果 `toString()` 被记录到日志中，可能泄露邮件服务器信息。

**后果**：日志中暴露邮件服务器配置信息，可能被用于钓鱼攻击。

**建议**：`toString()` 中对所有敏感字段（host/from/to）做脱敏处理，或直接不包含在 `toString()` 中。

---

### 4.7 [低] AesTool/DesTool 使用 ECB 模式

**位置**：`sh-tool/src/main/java/com/wkclz/tool/tools/AesTool.java` 和 `DesTool.java`

**问题**：AES 和 DES 加密均使用 ECB 模式（`AES/ECB/PKCS5Padding`、`DES/ECB/PKCS5Padding`）。ECB 模式对相同明文产生相同密文，无法抵御模式分析攻击。

**后果**：加密数据存在被模式分析破解的风险，特别是加密大量结构化数据时。

**建议**：改用 CBC 或 GCM 模式，配合随机 IV（初始化向量），密文中包含 IV 以便解密。

---

## 五、其他潜在风险

### 5.1 可观测性与运维

#### 5.1.1 [高] 缺乏结构化日志与链路追踪

**问题**：框架使用 SLF4J + Logback，但日志输出为非结构化文本，未集成 MDC（Mapped Diagnostic Context）或 TraceId。在微服务环境下，无法通过 TraceId 串联一次请求的完整调用链。

**后果**：分布式环境下故障排查困难，无法快速定位请求在哪个服务/方法出错。

**建议**：
- 在 Filter/Interceptor 中生成 TraceId 并写入 MDC。
- 日志输出格式增加 TraceId 字段。
- 集成 Micrometer 暴露 JVM/业务指标。

#### 5.1.2 [中] 动态数据源缺乏监控指标

**位置**：`sh-dynamicdb` 模块

**问题**：动态创建的数据源没有暴露连接池指标（活跃连接数、等待线程数、连接泄漏检测等），运维人员无法感知数据源健康状态。

**后果**：数据源连接池耗尽时无告警，业务请求超时后才发现问题。

**建议**：为每个动态数据源注册 Druid 的 JMX 指标，或集成 Micrometer 暴露 Prometheus 指标。

#### 5.1.3 [中] Redis 消息队列消费失败无重试机制

**位置**：`sh-redis/src/main/java/com/wkclz/redis/queue/RedisMessageQueueManager.java`

**问题**：消费线程捕获 `listener.onMessage()` 异常后仅记录日志，消息已被 BLPOP 弹出无法重新消费，等同于消息丢失。

**后果**：消息处理失败后永久丢失，业务数据不一致。

**建议**：
- 实现 Dead Letter Queue（DLQ），失败消息转入 DLQ 待人工处理。
- 增加重试机制（指数退避重试 3 次）。
- 考虑使用 Redis Stream 替代 List，支持 ACK/NACK 语义。

---

### 5.2 业务连续性与容错

#### 5.2.1 [高] 缺乏熔断、降级、限流机制

**问题**：框架未集成 Sentinel、Resilience4j 等熔断降级组件。当 Redis/MQTT/数据库等依赖服务不可用时，请求会持续超时或失败，无降级策略。

**后果**：依赖服务故障引发级联失败，系统整体不可用（雪崩效应）。

**建议**：
- 集成 Sentinel 或 Resilience4j，为 Redis/MQTT/数据库调用配置熔断规则。
- 为关键接口设计降级策略（如缓存降级、默认值返回）。
- 增加限流注解（如 `@RateLimiter`），防止突发流量击垮系统。

#### 5.2.2 [中] MQTT 断线重连无指数退避

**位置**：`sh-mqtt/src/main/java/com/wkclz/mqtt/config/MqttConfig.java` — `MqttReconnectCallback`

**问题**：MQTT 客户端使用 `automaticReconnect=true`，Eclipse Paho 内置的重连策略是固定间隔（1秒起步，最长2分钟），在 Broker 长时间不可用时，频繁重连浪费资源。

**后果**：Broker 故障期间，客户端持续重连消耗 CPU 和网络资源。

**建议**：自定义重连策略，实现指数退避（1s → 2s → 4s → ... → 最大 60s），并在重连失败 N 次后触发告警。

#### 5.2.3 [中] Redis 单点故障风险

**位置**：`sh-redis/src/main/java/com/wkclz/redis/config/RedisKeepAliveConfig.java`

**问题**：`RedisStandaloneConfiguration` 仅支持单机模式，不支持 Redis Cluster/Sentinel。单机 Redis 故障时，所有依赖 Redis 的功能（缓存、分布式锁、ID 生成、消息队列）不可用。

**后果**：Redis 单点故障导致系统大面积不可用。

**建议**：
- 支持 `RedisSentinelConfiguration` 和 `RedisClusterConfiguration`，通过配置切换。
- `RedisIdGenerator` 已有本地降级策略，但 `RedisHelper`/`RedisLock` 无降级，需补充。

---

### 5.3 数据一致性与可靠性

#### 5.3.1 [高] 逻辑删除与唯一约束冲突

**位置**：`sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/DeleteByIdMapperProvider.java`

**问题**：逻辑删除将 `deleted` 字段设为微秒级时间戳字符串，而非 `1/0` 布尔值。如果数据库表对业务字段（如 `username`）有唯一约束，删除后重新插入相同 `username` 会违反唯一约束（因为旧记录的 `username` 仍然存在，只是 `deleted` 值变了）。

**后果**：删除后无法重新创建相同业务标识的数据，影响业务流程。

**建议**：
- 唯一约束改为联合唯一索引：`UNIQUE KEY (username, deleted)`，其中 `deleted` 默认值为 `0`（未删除），删除后 `deleted` 为时间戳，不影响新记录插入。
- 或在文档中明确说明唯一约束需包含 `deleted` 字段。

#### 5.3.2 [中] 乐观锁更新失败无业务层提示

**位置**：`sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/UpdateByIdMapperProvider.java`

**问题**：乐观锁更新失败（`WHERE version = ?` 匹配不到记录）时，`updateById` 返回影响行数 0，但 `BaseService` 和 `UserRest` 未检查返回值，直接返回成功。

**后果**：并发更新时，后提交的更新静默失败，用户以为操作成功但数据未变更。

**建议**：在 `BaseService` 中封装更新方法，检查返回值，若影响行数为 0 则抛出 `SystemException(ResultCode.RECORD_NOT_EXIST_OR_OUT_OF_DATE)`。

#### 5.3.3 [中] Redis 消息队列非严格 FIFO

**位置**：`sh-redis/src/main/java/com/wkclz/redis/queue/RedisMessageQueueImpl.java`

**问题**：`sendMessage()` 使用 `lPush`（左推入队），`receiveMessage()` 使用 `bLPop`（左弹出队），实现的是 LIFO（后进先出）栈模式，而非 FIFO（先进先出）队列模式。若要 FIFO，应使用 `lPush` + `rPop` 或 `rPush` + `lPop`。

**后果**：消息消费顺序与发送顺序相反，先发送的消息最后被消费，可能影响业务逻辑。

**建议**：将 `bLPop` 改为 `bRPop`，或将 `lPush` 改为 `rPush`，实现严格的 FIFO。

---

### 5.4 合规与法律风险

#### 5.4.1 [中] 用户手机号脱敏策略未强制执行

**位置**：`sh-core/src/main/java/com/wkclz/core/base/UserInfo.java`

**问题**：`UserInfo.mobile` 字段注释标注"脱敏"，但框架层面无强制脱敏机制。`mobile` 的值是否脱敏完全取决于上游（拦截器/业务代码），若上游忘记脱敏，手机号会以明文形式传递到响应体、日志、缓存中。

**后果**：违反《个人信息保护法》对敏感个人信息的保护要求，可能面临法律风险。

**建议**：
- 在 `UserInfo.setMobile()` 方法中内置脱敏逻辑（如保留前 3 后 4 位）。
- 或使用自定义序列化器，在 JSON 输出时自动脱敏。

#### 5.4.2 [低] 开源组件许可证风险

**问题**：项目依赖 BouncyCastle（加密库），其许可证为 MIT，与 Apache 2.0 兼容。但 `sh-bom` 中未统一管理许可证信息，若未来引入 GPL 类组件，可能产生许可证冲突。

**后果**：许可证冲突可能导致法律纠纷或无法开源/商用。

**建议**：在 `sh-bom` 或独立文件中维护依赖许可证清单，引入新依赖时检查许可证兼容性。

---

### 5.5 代码质量与可维护性

#### 5.5.1 [高] 缺乏单元测试覆盖

**问题**：整个框架项目未发现任何测试目录（`src/test/java`）或测试文件。核心模块（SQL Provider、拦截器、分布式锁、ID 生成器等）均无单元测试。

**后果**：
- 代码重构时无法验证行为一致性，极易引入回归 Bug。
- AI Agent 基于故事开发后无法自动验证验收标准。

**建议**：
- 优先为高风险模块编写单元测试：`BaseMapperProvider`（SQL 生成正确性）、`RedisLock`（并发安全性）、`RedisIdGenerator`（唯一性）、`DynamicDataSource`（路由正确性）。
- 使用 Testcontainers 进行集成测试（Redis/MySQL/MQTT）。
- 在 CI 流水线中强制测试覆盖率阈值。

#### 5.5.2 [中] MQTT 配置前缀不一致

**位置**：`sh-mqtt/src/main/java/com/wkclz/mqtt/config/MqttConfig.java`

**问题**：MQTT 配置使用 `shrimp.cloud.mqtt.*` 前缀，而 AGENTS.md 中声明的配置前缀为 `sh.mqtt.*`，其他模块均使用 `sh.*` 前缀。配置前缀不一致导致使用困惑。

**后果**：开发者按文档配置 `sh.mqtt.*` 不生效，排查后发现实际前缀为 `shrimp.cloud.mqtt.*`。

**建议**：统一为 `sh.mqtt.*` 前缀，与框架其他模块保持一致。

#### 5.5.3 [中] 异常子类结构完全重复

**位置**：`sh-core/src/main/java/com/wkclz/core/exception/` 目录下 7 个子类

**问题**：`ApiException`、`ApplicationException`、`NotFoundException`、`SystemException`、`UnauthorizedException`、`UserException`、`ValidationException` 七个子类的代码结构完全相同（6 个构造函数 + 3 个静态工厂方法），仅类名不同。大量重复代码。

**后果**：修改异常体系时需同步修改 7 个文件，容易遗漏。

**建议**：使用代码生成（如 Lombok `@CustomLog` 风格的注解处理器）或模板方法模式消除重复。

#### 5.5.4 [低] AreaUtil 和 EnumUtil 整体注释未删除

**位置**：`sh-tool/src/main/java/com/wkclz/tool/utils/AreaUtil.java` 和 `EnumUtil.java`

**问题**：两个工具类的代码全部被注释，但文件仍保留在代码库中。注释代码增加维护负担，且可能误导开发者以为这些功能可用。

**后果**：代码库中存在死代码，降低可维护性。

**建议**：直接删除这两个文件，通过 Git 历史可找回。若需保留参考，移至 `src/main/java/.../deprecated/` 包下。

---

## 风险汇总矩阵

| 编号 | 风险等级 | 类别 | 摘要 | 模块 |
|------|---------|------|------|------|
| 1.1 | 🔴 高 | 性能 | SQL Provider 反射开销 | sh-mybatis |
| 1.2 | 🔴 高 | 性能 | 动态数据源 DCL 同步锁阻塞 | sh-dynamicdb |
| 2.1 | 🔴 高 | 内存 | ThreadLocal 泄漏风险 | sh-core/sh-dynamicdb/sh-web |
| 2.2 | 🔴 高 | 内存 | 动态数据源连接池未关闭 | sh-dynamicdb |
| 3.1 | 🔴 高 | 并发 | 异步创建线程饥饿死锁 | sh-dynamicdb |
| 3.2 | 🔴 高 | 并发 | 分布式锁过期与业务执行不匹配 | sh-redis |
| 4.1 | 🔴 高 | 安全 | SQL 注入：updateBy 字符串拼接 | sh-mybatis |
| 4.2 | 🔴 高 | 安全 | fastjson2 AutoType 反序列化漏洞 | sh-redis |
| 4.3 | 🔴 高 | 安全 | 敏感配置明文存储 | sh-spring |
| 5.1.1 | 🔴 高 | 可观测性 | 缺乏链路追踪与结构化日志 | 全局 |
| 5.2.1 | 🔴 高 | 容错 | 缺乏熔断降级限流机制 | 全局 |
| 5.3.1 | 🔴 高 | 数据一致性 | 逻辑删除与唯一约束冲突 | sh-mybatis |
| 5.5.1 | 🔴 高 | 代码质量 | 零单元测试覆盖 | 全局 |
| 1.3 | 🟡 中 | 性能 | RedisHelper 写操作吞异常 | sh-redis |
| 1.4 | 🟡 中 | 性能 | UserNameBodyAdvice 递归反射 | sh-web |
| 1.5 | 🟡 中 | 性能 | 双分页策略并存 | sh-mybatis |
| 2.3 | 🟡 中 | 内存 | 消息队列线程池拒绝策略 | sh-redis |
| 2.4 | 🟡 中 | 内存 | MqttHandlerFactory 静态 Map | sh-mqtt |
| 3.3 | 🟡 中 | 并发 | 雪花 ID 时钟回拨粗暴处理 | sh-tool |
| 3.4 | 🟡 中 | 并发 | FreeMarker Configuration 非线程安全 | sh-spring |
| 4.4 | 🟡 中 | 安全 | RemoveReq 注解语义冲突 | sh-web |
| 4.5 | 🟡 中 | 安全 | 日志脱敏未保留部分信息 | sh-core |
| 4.6 | 🟡 中 | 安全 | MailUtil toString 脱敏不彻底 | sh-spring |
| 4.7 | 🟡 中 | 安全 | AES/DES 使用 ECB 模式 | sh-tool |
| 5.1.2 | 🟡 中 | 可观测性 | 动态数据源缺乏监控指标 | sh-dynamicdb |
| 5.1.3 | 🟡 中 | 可观测性 | 消息队列消费失败无重试 | sh-redis |
| 5.2.2 | 🟡 中 | 容错 | MQTT 重连无指数退避 | sh-mqtt |
| 5.2.3 | 🟡 中 | 容错 | Redis 单点故障风险 | sh-redis |
| 5.3.2 | 🟡 中 | 数据一致性 | 乐观锁更新失败无提示 | sh-mybatis |
| 5.3.3 | 🟡 中 | 数据一致性 | 消息队列非严格 FIFO | sh-redis |
| 5.4.1 | 🟡 中 | 合规 | 手机号脱敏未强制执行 | sh-core |
| 5.5.2 | 🟡 中 | 代码质量 | MQTT 配置前缀不一致 | sh-mqtt |
| 5.5.3 | 🟡 中 | 代码质量 | 异常子类代码完全重复 | sh-core |
| 2.5 | 🟢 低 | 内存 | BeanUtil 反射缓存无过期 | sh-tool |
| 3.5 | 🟢 低 | 并发 | RedisIdGenerator 非原子操作 | sh-redis |
| 5.4.2 | 🟢 低 | 合规 | 开源许可证风险 | sh-bom |
| 5.5.4 | 🟢 低 | 代码质量 | 注释代码未清理 | sh-tool |

---

## 优先修复建议（Top 5）

| 优先级 | 风险编号 | 修复内容 | 预估影响 |
|--------|---------|---------|---------|
| P0 | 4.1 | SQL Provider 中 updateBy 改用 `#{updateBy}` 参数绑定 | 消除 SQL 注入风险 |
| P0 | 4.2 | 禁用 fastjson2 AutoType 或配置白名单 | 消除远程代码执行风险 |
| P0 | 2.1 | 统一 ThreadLocal 清理机制，增加 Filter 自动清理 | 消除内存泄漏与数据错乱 |
| P0 | 5.5.1 | 为核心模块编写单元测试 | 保障代码质量与重构安全 |
| P1 | 1.2 | 动态数据源改用 `computeIfAbsent` 替代全局锁 | 消除多租户性能瓶颈 |
