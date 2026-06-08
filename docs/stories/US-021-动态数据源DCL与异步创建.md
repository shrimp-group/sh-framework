# 动态数据源 DCL 与异步创建
- **所属模块**：sh-dynamicdb
- **优先级**：中
- **故事ID**：US-021

## 1. 用户故事 (User Story)
**作为** 框架开发者，
**我希望** 动态数据源创建使用 DCL 双重检查锁 + CompletableFuture 异步创建，
**以便于** 避免并发重复创建数据源，同时防止默认数据源管理元信息时的递归死循环。

## 流程图

```mermaid
flowchart TD
    A[determineCurrentLookupKey] --> B[从 ThreadLocal 获取 key]
    B --> C{resolvedDataSources 中存在?}
    C -->|是| D[返回已有数据源]
    C -->|否| E{dataSourceFutures 中存在?}
    E -->|是| F[等待 CompletableFuture 完成]
    F --> G{创建成功?}
    G -->|是| H[返回新数据源]
    G -->|否| I[移除 Future, 允许重试<br/>抛出 SystemException]
    E -->|否| J[DCL: 双重检查锁]
    J --> K[创建 CompletableFuture]
    K --> L[提交到专用线程池异步创建]
    L --> M[在新线程中执行]
    M --> N[DynamicDataSourceFactory.createDataSource]
    N --> O{返回 null?}
    O -->|是| P[抛出 SystemException]
    O -->|否| Q[创建 DruidDataSource<br/>存入 resolvedDataSources]
    Q --> H
```

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 多线程并发请求同一数据源 key, When 第一个线程加锁创建, Then 其他线程通过 DCL 检查直接使用已创建的数据源
- [场景2] Given 默认数据源管理第三方数据源元信息, When 当前线程查询元信息, Then CompletableFuture 在新线程中执行，新线程 ThreadLocal 为空，自动回退到默认数据源
- [场景3] Given 数据源创建失败, When CompletableFuture.get() 抛出异常, Then 向上传播 SystemException
- [异常场景] Given 数据源 key 对应的 DynamicDataSourceFactory.createDataSource() 返回 null, When 异步创建执行, Then 抛出 SystemException("can not find dataSource by key")

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/DynamicDataSource.java` (determineCurrentLookupKey核心方法，DCL+异步)
- `sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/AbstractShrimpRoutingDataSource.java` (路由数据源基类，ConcurrentHashMap支持)
- `sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/bean/DefaultDataSourceConfig.java` (默认数据源配置，复用连接池参数)
- `sh-dynamicdb/src/main/java/com/wkclz/dynamicdb/config/DynamicDataSourceConfig.java` (缓存时间配置)
