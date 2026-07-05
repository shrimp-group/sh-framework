# IAM 契约层（认证/鉴权/AK 签名/SSO 门面）
- **所属模块**：sh-iam-contract
- **优先级**：高
- **故事ID**：US-031

## 1. 用户故事 (User Story)
**作为** 需要集成认证鉴权能力的业务系统开发者，
**我希望** 引入轻量级的 IAM 契约层（API 模块零业务依赖，Default 模块提供读宽容验证严格的默认实现），
**以便于** 业务系统按需引入或替换实现，避免强制依赖 JWT/Redis/HTTP Client 等重依赖。

## 流程图

```mermaid
flowchart TD
    A[HTTP Request] --> B[DefaultAuthFilter]
    B --> C{路径匹配?}
    C -->|匹配 /*/public/**| PASS[直接放行]
    C -->|根路径 /| FORBIDDEN[返回 403 Forbidden]
    C -->|其他路径| D[调用 AuthContract.authenticate]

    D --> E[AuthContract 实现方<br/>业务方实现或默认实现]
    E --> F{AuthResult?}
    F -->|返回 null 无 token| UNAUTHORIZED[返回 401 Unauthorized]
    F -->|返回 AuthResult| G[AuthResult = Principal + Session]

    G --> H[PrincipalContext.cache 缓存上下文]
    H --> I[业务代码读取 PrincipalContext.getPrincipal]
    I --> J[业务代码调用 AuthzContract.canAccessApi]

    J --> K{鉴权结果?}
    K -->|业务方实现 AuthzContract| L[按业务策略返回布尔]
    K -->|未实现 默认实现| M[抛 AuthException ACCESS_DENIED]

    subgraph RPC[服务间 RPC]
        N[AkSignContract.sign 签名]
        O[AkSignContract.verifySign 验签]
    end

    subgraph SSO[客户端 SSO 交互]
        P[SsoFacadeContract.login 登录]
        Q[SsoFacadeContract.saveLog 记录日志]
        R[SsoFacadeContract.logout 登出]
    end

    L --> S[业务处理]
    S --> T[finally PrincipalContext.clear 清理上下文]

    PASS --> T
    FORBIDDEN --> T
    UNAUTHORIZED --> T
    M --> T

    style E fill:#fff3cd
    style K fill:#fff3cd
    style M fill:#f8d7da
    style FORBIDDEN fill:#f8d7da
    style UNAUTHORIZED fill:#f8d7da
```

> **说明**：
> - `AuthContract` / `AuthzContract` / `AkSignContract` / `SsoFacadeContract` 四契约由业务方实现，通过 `@ConditionalOnMissingBean` 替换默认实现。
> - `DefaultAuthContract` 读宽容（无 token 返回 null），`DefaultAuthzContract` 验证严格（`canAccessApi` 抛 `ACCESS_DENIED`）。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 业务系统引入 iam-contract-default 依赖, When Spring Boot 启动, Then IamContractAutoConfig 通过 @ConditionalOnMissingBean 注册四个默认实现 + DefaultAuthFilter
- [场景2] Given 业务系统实现 AuthContract 接口并声明 @Component, When Spring 启动, Then 业务实现 Bean 替换 DefaultAuthContract（@ConditionalOnMissingBean 阻止默认 Bean 注册）
- [场景3] Given DefaultAuthFilter 拦截请求, When 请求路径匹配 `/*/public/**`, Then 直接放行（不调用认证）
- [场景4] Given DefaultAuthFilter 拦截请求, When 请求路径为 `/`（根路径）, Then 返回 403 Forbidden
- [场景5] Given DefaultAuthFilter 拦截请求, When AuthContract.authenticate() 返回 null（无 token）, Then 返回 401 Unauthorized
- [异常场景] Given DefaultAuthzContract.canAccessApi() 被调用, When 业务系统未实现 AuthzContract, Then 抛出 AuthException(ACCESS_DENIED) 防止裸奔

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：

契约 API 模块（sh-iam-contract/iam-contract-api）：
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Principal.java` (用户主体，JWT claims 映射)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Session.java` (会话信息，含 authType/authIdentifier)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/AuthResult.java` (认证结果 = Principal + Session)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/context/PrincipalContext.java` (Principal 读取上下文，基于 RequestContextHolder + ThreadLocal 双存储)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/config/ContractSettings.java` (静态配置持有器，供 default 方法访问)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthContract.java` (认证契约 SPI)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthzContract.java` (鉴权契约 SPI，六维度重载)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AkSignContract.java` (AK 签名契约 SPI)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/facade/SsoFacadeContract.java` (SSO 门面契约 SPI)
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/exception/AuthException.java` (契约层异常 + AuthErrorType 枚举)

默认实现模块（sh-iam-contract/iam-contract-default）：
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContract.java` (认证默认实现：读宽容验证严格)
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContract.java` (鉴权默认实现：读返回空列表，canAccessApi 抛 ACCESS_DENIED)
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContract.java` (AK 签名默认实现：sign/verifySign 抛异常)
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContract.java` (SSO 门面默认实现：login 抛异常，saveLog/logout 静默)
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/filter/DefaultAuthFilter.java` (鉴权过滤器：根路径拒绝/public 放行/调用 SPI/缓存上下文/finally 清理)
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/ContractConfig.java` (@Value 配置绑定 + @PostConstruct 同步 ContractSettings)
- `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/IamContractAutoConfig.java` (@AutoConfiguration + @ConditionalOnMissingBean 注册默认实现)
- `sh-iam-contract/iam-contract-default/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (自动配置注册)
