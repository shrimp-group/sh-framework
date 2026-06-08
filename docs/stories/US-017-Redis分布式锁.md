# Redis 分布式锁
- **所属模块**：sh-redis
- **优先级**：高
- **故事ID**：US-017

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 使用 RedisLock 实现分布式锁（SETNX + Lua 原子释放），
**以便于** 在分布式环境下安全地控制共享资源的并发访问。

## 流程图

```mermaid
sequenceDiagram
    participant T1 as 线程1
    participant RL as RedisLock
    participant R as Redis
    participant WD as Watchdog

    T1->>RL: tryLock("order:123", 30s)
    RL->>R: SETNX + EXPIRE (原子操作)
    R-->>RL: 成功, 返回 requestId(UUID)
    RL-->>T1: 返回 requestId

    RL->>WD: 启动 Watchdog 定时续期
    Note over WD: 续期间隔 = lockTime/3 = 10s
    WD->>R: EXPIRE 重置过期时间
    Note over WD: 持续续期直到释放

    T1->>RL: releaseLock("order:123", requestId)
    RL->>R: 执行 Lua 脚本
    Note over R: IF requestId匹配 THEN DELETE ELSE 什么都不做
    R-->>RL: 释放成功
    RL->>WD: 停止 Watchdog
    RL-->>T1: 锁已释放

    Note over T1,R: 重试场景
    participant T2 as 线程2
    T2->>RL: tryLockWithRetry("order:123", 30s, 3次, 1s)
    RL->>R: SETNX (第1次)
    R-->>RL: 失败(锁已被持有)
    Note over RL: 等待1秒
    RL->>R: SETNX (第2次)
    R-->>RL: 失败
    Note over RL: 等待1秒
    RL->>R: SETNX (第3次)
    R-->>RL: 成功/失败
    RL-->>T2: 返回结果
```

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 调用 redisLock.tryLock("order:123", 30, TimeUnit.SECONDS), When 获取锁成功, Then 返回 UUID 作为 requestId
- [场景2] Given 锁的持有者调用 redisLock.releaseLock("order:123", requestId), When Lua 脚本执行, Then 只有 requestId 匹配时才删除锁，防止误删其他线程的锁
- [场景3] Given 锁已被其他线程持有, When 调用 redisLock.tryLockWithRetry("order:123", 30s, 3次, 1秒间隔), Then 最多重试 3 次，每次间隔 1 秒
- [异常场景] Given 锁持有者未释放锁但锁已过期, When 其他线程获取锁, Then 成功获取（SETNX + EXPIRE 原子操作防止死锁）

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-redis/src/main/java/com/wkclz/redis/helper/RedisLock.java` (分布式锁，SETNX+Lua原子释放+重试)
- `sh-redis/src/main/java/com/wkclz/redis/helper/RedisHelper.java` (setIfAbsent方法，SETNX原子操作)
