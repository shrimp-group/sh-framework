# IAM 契约层设计规格

> 日期: 2026-07-04
> 状态: 已实现（契约层已迁移至 sh-framework），iam-sdk 重构待后续
> 范围: sh-iam-contract/iam-contract-api + sh-iam-contract/iam-contract-default 模块
> 迁移说明: 本文档迁移自 sh-iam 项目（/docs/superpowers/specs/2026-07-04-iam-contract-layer-design.md），并根据 sh-framework 实际代码状态完成本地化调整

## 1. 背景与动机

当前 `iam-sdk` 将认证过滤器、会话读取、AK 签名、SSO RPC、验证码等能力捆在一个模块中，导致只想读取用户信息的轻量模块被迫依赖 JWT、Redis、HTTP Client 等所有重依赖。

**核心问题**: 耦合导致引入成本高，无法按需使用。

**解决方案**: 抽象契约层（Contract Layer），定义接口与中性模型，提供零业务依赖的默认实现，具体实现（如 iam-sdk）通过 `@ConditionalOnMissingBean` 自动替换默认实现。

**行业对标**: Spring Security（SecurityFilterChain + UserDetailsService）、Apache Shiro（Realm + SessionManager）、SA-Token（StpLogic），均为"契约接口 + 默认实现 + @ConditionalOnMissingBean 替换"模式。

## 2. 设计决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 模块划分 | API + Default 双模块 | API 零依赖可独立引入；Default 提供开箱即用 |
| 契约范围 | Auth + Authz + AkSign + SsoFacade 四契约 | 覆盖认证、鉴权、签名、远程调用全场景 |
| 默认实现 | 读宽容、验证严格 | 读返回空/原值不影响启动；验证/鉴权拒绝防止裸奔 |
| 数据模型 | 契约层新建中性 DTO | 不依赖 iam-common，实现层负责映射 |
| 过滤器 | 契约层提供 + SPI | 业务方只需实现 SPI，无需重复写过滤器 |
| Principal 读取 | PrincipalContext 独占取代 UserContext | 统一入口，不再依赖 sh-core UserContext |
| 包名 | `com.wkclz.iam.contract` / `.defaults` | 简洁，与 iam-sdk 命名对齐 |
| tenantCode | 不属于 Principal，从请求头动态获取 | 租户可随时切换，是运行时上下文而非用户身份 |
| iam-sdk 重构 | 本次不涉及，提供实现要求文档 | 契约层独立交付，降低风险 |

## 3. 模块架构

### 3.1 模块拓扑

```
┌─────────────────────────────────────────────────────────────┐
│  sh-core / sh-web / sh-tool / sh-redis  (sh-* 框架包)        │
└──────────────────────────────┬──────────────────────────────┘
                               │
                ┌──────────────┴──────────────┐
                │                             │
        ┌───────▼────────┐          ┌─────────▼─────────┐
        │ iam-contract-  │          │ iam-contract-     │
        │     api        │◄─────────│     default       │
        │                │          │                   │
        │ · 契约接口      │          │ · Default* 实现    │
        │ · 中性模型      │          │ · DefaultAuthFilter│
        │ · PrincipalCtx │          │ · AutoConfig      │
        │ · 异常/枚举     │          │  (@CondOnMissing) │
        └───────┬────────┘          └─────────┬─────────┘
                │                             │
                │   ┌─────────────────────────┘
                │   │ (实现层提供 Bean 后，default 自动失效)
                ▼   ▼
        ┌─────────────────────────────────────────┐
        │  iam-sdk (后续重构任务)                   │
        │  · IamAuthContractImpl                  │
        │  · IamAuthzContractImpl                 │
        │  · IamAkSignContractImpl                │
        │  · IamSsoFacadeContractImpl             │
        └─────────────────────────────────────────┘
```

### 3.2 依赖规则

| 模块 | 可依赖 | 不可依赖 |
|------|--------|----------|
| `iam-contract-api` | `sh-tool`（仅加密工具，可选）；`spring-boot-starter-web`（provided scope，提供 HttpServletRequest 类型） | 任何 iam-* 模块、sh-web、Redis、JWT、HTTP 客户端 |
| `iam-contract-default` | `iam-contract-api` + 上述 sh-* | 任何 iam-* 业务模块、iam-common |
| `iam-sdk`（后续） | `iam-contract-api` + sh-* + JWT + Redis + HTTP | iam-common（如需，通过映射而非直接依赖） |

> 说明：迁移至 sh-framework 后，iam-contract-api 不再依赖 sh-web，改为通过 Spring 的 `RequestContextHolder` 获取 HttpServletRequest，由 `spring-boot-starter-web` provided scope 提供类型。

### 3.3 包结构

```
sh-iam-contract/iam-contract-api/   基础包: com.wkclz.iam.contract
  ├─ bean/                # 数据模型（中性 DTO）
  │    ├─ Principal.java          # 用户主体
  │    ├─ Session.java            # 会话信息
  │    ├─ AuthResult.java         # 认证结果（Principal + Session）
  │    ├─ Tenant.java             # 租户
  │    ├─ App.java                # 应用
  │    ├─ Menu.java               # 菜单（树形）
  │    ├─ Api.java                # API 路由
  │    ├─ FieldPermission.java    # 字段权限
  │    ├─ DataDimension.java      # 数据维度
  │    ├─ RequestLog.java         # 请求日志
  │    ├─ req/
  │    │    └─ SessionCreateReq.java
  │    └─ resp/
  │         └─ LoginResp.java
  ├─ facade/              # 门面契约
  │    └─ SsoFacadeContract.java
  ├─ service/             # 服务契约（SPI）
  │    ├─ AuthContract.java
  │    ├─ AuthzContract.java
  │    └─ AkSignContract.java
  ├─ context/             # 上下文工具
  │    └─ PrincipalContext.java
  ├─ config/              # 静态配置持有器
  │    └─ ContractSettings.java  # 静态配置（由 AutoConfig 初始化）
  ├─ enums/               # 枚举
  │    └─ AuthScene.java
  └─ exception/           # 异常
       └─ AuthException.java


sh-iam-contract/iam-contract-default/   基础包: com.wkclz.iam.contract.defaults
  ├─ facade/
  │    └─ DefaultSsoFacadeContract.java
  ├─ service/
  │    ├─ DefaultAuthContract.java
  │    ├─ DefaultAuthzContract.java
  │    └─ DefaultAkSignContract.java
  ├─ filter/
  │    └─ DefaultAuthFilter.java
  ├─ context/
  │    └─ DefaultPrincipalContext.java
  └─ config/
       ├─ ContractConfig.java              # @ConfigurationProperties 绑定 yaml
       └─ IamContractAutoConfig.java       # @ConditionalOnMissingBean 注册 + @PostConstruct 初始化 ContractSettings
```

## 4. 契约接口设计

### 4.1 AuthContract（认证契约）

```java
package com.wkclz.iam.contract.service;

public interface AuthContract {

    /**
     * 从 HTTP 请求中认证用户（过滤器主入口）
     *
     * 实现职责:
     * 1. 从请求头提取 token（Authorization / token，去 Bearer 前缀）
     * 2. 校验 JWT 签名与有效期
     * 3. 校验 Session 存在性（如 Redis）
     * 4. 返回 Principal + Session
     *
     * @param request HTTP 请求
     * @return 认证结果；token 不存在时返回 null（过滤器据此放行 public 路径）
     * @throws AuthException token 无效、签名错误、会话过期等
     */
    AuthResult authenticate(HttpServletRequest request);

    /**
     * 校验 token（非 HTTP 场景：WebSocket、定时任务等）
     *
     * @param token          JWT token
     * @param authIdentifier 认证标识符
     * @return 会话信息
     * @throws AuthException token 无效或会话过期
     */
    Session checkToken(String token, String authIdentifier);
}
```

### 4.2 AuthzContract（鉴权契约）

```java
package com.wkclz.iam.contract.service;

public interface AuthzContract {

    // ── 1. 租户 ──

    /** 完整参数 */
    List<Tenant> listTenants(Principal principal);

    /** 上下文重载 */
    default List<Tenant> listTenants() {
        return listTenants(PrincipalContext.getPrincipal());
    }

    // ── 2. 应用 ──

    /** 完整参数 */
    List<App> listApps(Principal principal, String tenantCode);

    /** 上下文重载 */
    default List<App> listApps() {
        return listApps(PrincipalContext.getPrincipal(), PrincipalContext.getTenantCode());
    }

    default List<App> listApps(String tenantCode) {
        return listApps(PrincipalContext.getPrincipal(), tenantCode);
    }

    // ── 3. 菜单树 ──

    /** 完整参数 */
    List<Menu> getMenuTree(Principal principal, String appCode);

    /** 上下文重载 */
    default List<Menu> getMenuTree() {
        return getMenuTree(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode());
    }

    default List<Menu> getMenuTree(String appCode) {
        return getMenuTree(PrincipalContext.getPrincipal(), appCode);
    }

    // ── 4. 接口鉴权 ──

    /** 完整参数 */
    boolean canAccessApi(Principal principal, String appCode, String apiUri, String apiMethod);

    /** 上下文重载 */
    default boolean canAccessApi(String apiUri, String apiMethod) {
        return canAccessApi(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(), apiUri, apiMethod);
    }

    default boolean canAccessApi(HttpServletRequest request) {
        return canAccessApi(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(),
                            request.getRequestURI(), request.getMethod());
    }

    // ── 5. 字段权限 ──

    /** 完整参数 */
    List<FieldPermission> listFieldPermissions(Principal principal, String appCode, String menuCode);

    /** 上下文重载 */
    default List<FieldPermission> listFieldPermissions(String menuCode) {
        return listFieldPermissions(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(), menuCode);
    }

    /** 完整参数：字段过滤 */
    List<String> filterFields(Principal principal, String appCode, String menuCode, List<String> fields);

    /** 上下文重载 */
    default List<String> filterFields(String menuCode, List<String> fields) {
        return filterFields(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(), menuCode, fields);
    }

    // ── 6. 数据权限 ──

    /** 完整参数 */
    List<DataDimension> getDataDimensions(Principal principal, String appCode);

    /** 上下文重载 */
    default List<DataDimension> getDataDimensions() {
        return getDataDimensions(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode());
    }

    default List<DataDimension> getDataDimensions(String appCode) {
        return getDataDimensions(PrincipalContext.getPrincipal(), appCode);
    }
}
```

**重载规则**:

| 场景 | 调用方式 | 适用 |
|------|----------|------|
| 完整参数 | `canAccessApi(principal, appCode, uri, method)` | 单元测试、非 HTTP 场景 |
| 上下文自动获取 | `canAccessApi(uri, method)` | 业务代码（最常用） |
| 请求全自动 | `canAccessApi(request)` | 过滤器/拦截器 |

### 4.3 AkSignContract（AK 签名契约）

```java
package com.wkclz.iam.contract.service;

public interface AkSignContract {

    /** 客户端签名：显式传入 appId + appSecret */
    String sign(String appId, String appSecret);

    /** 客户端签名重载：从 ContractSettings 获取 appId + appSecret */
    default String sign() {
        return sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
    }

    /** 服务端验签：显式传入参数 */
    boolean verifySign(String sign, String publicKey, String expectedAppId);

    /** 服务端验签重载：从请求头 + ContractSettings 自动获取参数 */
    default boolean verifySign(HttpServletRequest request) {
        String sign = request.getHeader("sign");
        String appId = request.getHeader("app-id");
        return verifySign(sign, ContractSettings.getPublicKey(), appId);
    }
}
```

> **注意**: `ContractSettings` 是 api 模块中的静态配置持有器，由 `IamContractAutoConfig` 在启动时通过 `@PostConstruct` 初始化。default 方法无法访问 Spring 上下文，因此通过静态持有器桥接。

### 4.4 SsoFacadeContract（SSO RPC 门面契约）

```java
package com.wkclz.iam.contract.facade;

public interface SsoFacadeContract {

    /** 远程登录 */
    LoginResp login(SessionCreateReq req);

    /** 远程保存请求日志 */
    void saveLog(RequestLog log);

    /** 远程登出（指定 token） */
    void logout(String token);

    /** 远程登出（从 PrincipalContext 获取 token） */
    default void logout() {
        logout(PrincipalContext.getToken());
    }
}
```

### 4.5 PrincipalContext（Principal 读取上下文）

```java
package com.wkclz.iam.contract.context;

public final class PrincipalContext {

    // ── 写入（过滤器调用） ──

    /** 缓存 Principal + Session 到当前请求上下文 */
    public static void cache(HttpServletRequest request, Principal principal, Session session);

    /** 清理上下文（请求结束时调用） */
    public static void clear();

    // ── 核心读取 ──

    public static Principal getPrincipal();
    public static Session getSession();

    // ── 便捷方法 ──

    public static String getUserCode();         // Principal.userCode
    public static String getUsername();         // Principal.username
    public static String getNickname();         // Principal.nickname
    public static String getTenantCode();       // 请求头 tenant-code（动态值）
    public static String getAppCode();          // 请求头 app-code
    public static String getToken();            // 请求头 Authorization / token
    public static String getAuthIdentifier();   // Session.authIdentifier

    // ── 路径匹配 ──

    public static boolean match(String pattern, String uri);
}
```

**数据来源映射**:

| 便捷方法 | 数据来源 | 获取方式 |
|----------|----------|----------|
| `getUserCode` | Principal.userCode | `getPrincipal().getUserCode()` |
| `getUsername` | Principal.username | `getPrincipal().getUsername()` |
| `getNickname` | Principal.nickname | `getPrincipal().getNickname()` |
| `getTenantCode` | 请求头 `tenant-code` | `request.getHeader("tenant-code")` |
| `getAppCode` | 请求头 `app-code` | `request.getHeader("app-code")` |
| `getToken` | 请求头 `Authorization` / `token` | 去 `Bearer ` 前缀 |
| `getAuthIdentifier` | Session.authIdentifier | `getSession().getAuthIdentifier()` |

> HttpServletRequest 获取方式：通过 Spring 的 `RequestContextHolder.getRequestAttributes()` + `ServletRequestAttributes` 获取，不再依赖 sh-web 的 RequestHelper。

**tenantCode 设计**: 租户是运行时动态切换值（用户可随时切换租户），不属于用户身份，因此不在 Principal 中。优先从请求头获取，非 HTTP 场景需显式传入。

**双存储策略**:
- `request.setAttribute`: 主存储，跟随请求生命周期，Servlet 规范保证线程安全
- `ThreadLocal`: 辅助存储，支持子线程读取（异步场景），由 `clear()` 在 finally 中清理

## 5. 数据模型设计

### 5.1 Principal（用户主体）

```java
@Data
@Schema(description = "用户主体")
public class Principal implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像")
    private String avatar;
}
```

字段来源: JWT claims 解析。不含 tenantCode（租户是动态切换值，从请求头获取）。

### 5.2 Session（会话信息）

```java
@Data
@Schema(description = "用户会话")
public class Session implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "认证类型：PASSWORD / LDAP / OAUTH 等")
    private String authType;

    @Schema(description = "认证标识符")
    private String authIdentifier;
}
```

精简原则: 只保留 JWT 无法携带的动态会话数据。username/nickname 已在 Principal 中。

### 5.3 AuthResult（认证结果）

```java
@Data
@Schema(description = "认证结果")
public class AuthResult implements Serializable {

    @Schema(description = "用户主体")
    private Principal principal;

    @Schema(description = "会话信息")
    private Session session;
}
```

### 5.4 Tenant

```java
@Data
@Schema(description = "租户")
public class Tenant implements Serializable {

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;
}
```

### 5.5 App

```java
@Data
@Schema(description = "应用")
public class App implements Serializable {

    @Schema(description = "应用编码")
    private String appCode;

    @Schema(description = "应用名称")
    private String appName;

    @Schema(description = "应用图标")
    private String icon;
}
```

### 5.6 Menu（树形结构）

```java
@Data
@Schema(description = "菜单")
public class Menu implements Serializable {

    @Schema(description = "菜单编码")
    private String menuCode;

    @Schema(description = "父级菜单编码")
    private String parentCode;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "菜单类型：MENU / BUTTON")
    private String menuType;

    @Schema(description = "路由路径")
    private String routePath;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "按钮编码（menuType=BUTTON 时）")
    private String buttonCode;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "子菜单列表")
    private List<Menu> children;
}
```

仅包含核心展示字段，不含管理字段。树构建由实现层负责。

### 5.7 Api

```java
@Data
@Schema(description = "API 路由")
public class Api implements Serializable {

    @Schema(description = "API 编码")
    private String apiCode;

    @Schema(description = "API 名称")
    private String apiName;

    @Schema(description = "HTTP 方法")
    private String apiMethod;

    @Schema(description = "URI 路径")
    private String apiUri;

    @Schema(description = "是否写操作")
    private Boolean writeFlag;
}
```

### 5.8 FieldPermission

```java
@Data
@Schema(description = "字段权限")
public class FieldPermission implements Serializable {

    @Schema(description = "字段编码")
    private String fieldCode;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "是否可见")
    private Boolean visible;

    @Schema(description = "是否可编辑")
    private Boolean editable;
}
```

### 5.9 DataDimension

```java
@Data
@Schema(description = "数据权限维度")
public class DataDimension implements Serializable {

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "授权值列表")
    private List<String> authorizedValues;
}
```

authorizedValues 为通用值列表，业务层根据 dimensionCode 解释含义。

### 5.10 RequestLog

```java
@Data
@Schema(description = "请求日志")
public class RequestLog implements Serializable {

    private String uri;
    private String method;
    private String requestBody;
    private Integer responseStatus;
    private String responseBody;
    private Long requestTime;
    private Long responseTime;
    private Long duration;
    private String clientIp;
    private String userCode;
    private String appCode;
}
```

### 5.11 SessionCreateReq

```java
@Data
@Schema(description = "会话创建请求")
public class SessionCreateReq implements Serializable {

    private String userCode;
    private String username;
    private String nickname;
    private String authType;
    private String authIdentifier;
    private String clientIp;
    private String userAgent;
}
```

### 5.12 LoginResp

```java
@Data
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    private String token;
    private String userCode;
    private String username;
    private String nickname;
}
```

### 5.13 辅助类型

**AuthException**:

```java
public class AuthException extends RuntimeException {

    private final AuthErrorType errorType;

    public enum AuthErrorType {
        TOKEN_MISSING,       // token 不存在
        TOKEN_INVALID,       // JWT 签名无效
        TOKEN_EXPIRED,       // JWT 过期
        SESSION_EXPIRED,     // 会话过期
        AK_SIGN_INVALID,    // AK 签名无效
        AK_SIGN_EXPIRED,    // AK 签名过期
        AK_NONCE_REPLAY,    // nonce 重放
        ACCESS_DENIED       // 接口鉴权拒绝
    }

    public AuthException(AuthErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public AuthErrorType getErrorType() { return errorType; }
}
```

**AuthScene**:

```java
public enum AuthScene {
    TOKEN,      // JWT Token 认证
    AK_SIGN,    // AK 签名认证
    PUBLIC      // 公开接口（无需认证）
}
```

### 5.14 模型映射参考

| 契约模型 | 现有类 | 主要差异 |
|----------|--------|----------|
| `Principal` | `UserJwt` + `UserInfo` | 合并，无 tenantCode |
| `Session` | `UserSession` | 精简，去除与 Principal 重复字段 |
| `Menu` | `IamMenu` | 去除管理字段，增加 children |
| `Api` | `IamApi` | 去除管理字段 |
| `Tenant` | 无对应 | 新增 |
| `App` | `IamApp` | 仅保留核心展示字段 |
| `RequestLog` | `sdk.RequestLog` | 字段对齐 |
| `SessionCreateReq` | `sdk.bean.req.SessionCreateReq` | 字段对齐 |
| `LoginResp` | `sdk.bean.resp.LoginResp` | 字段对齐 |

## 6. Default 实现设计

### 6.1 行为总表

| 默认实现 | 读操作 | 验证/写操作 |
|----------|--------|------------|
| `DefaultAuthContract` | `authenticate()` 无 token 返回 null | `checkToken()` 抛 AuthException(TOKEN_MISSING) |
| `DefaultAuthzContract` | 返回空列表；filterFields 返回原字段 | `canAccessApi()` 抛 AuthException(ACCESS_DENIED) |
| `DefaultAkSignContract` | -- | sign/verifySign 抛异常 |
| `DefaultSsoFacadeContract` | -- | saveLog/logout 静默跳过；login 抛异常 |
| `DefaultAuthFilter` | -- | 调用 AuthContract SPI，认证失败返回 401 |

### 6.2 DefaultAuthContract

- `authenticate()`: 无 token 返回 null；有 token 无实现则抛 AuthException(TOKEN_INVALID)
- `checkToken()`: 抛 AuthException(TOKEN_MISSING)
- 日志: warn 级别

### 6.3 DefaultAuthzContract

- 读操作: 返回 `Collections.emptyList()`，filterFields 返回原 fields
- `canAccessApi()`: 抛 AuthException(ACCESS_DENIED) -- 接口鉴权严格拒绝
- 日志: 读操作 debug，鉴权拒绝 warn

### 6.4 DefaultAkSignContract

- `sign()`: 抛 UnsupportedOperationException
- `verifySign()`: 抛 AuthException(AK_SIGN_INVALID)
- 日志: warn 级别

### 6.5 DefaultSsoFacadeContract

- `login()`: 抛 UnsupportedOperationException
- `saveLog()`: 静默跳过（日志丢失不阻断业务）
- `logout()`: 静默跳过
- 日志: login warn，其余 debug

### 6.6 DefaultAuthFilter

```java
@Slf4j
@Component
public class DefaultAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthContract authContract;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 根路径拒绝
        if ("/".equals(uri)) {
            ResponseHelper.responseError(response, HttpStatus.FORBIDDEN, "Forbidden");
            return;
        }

        // 2. public 路径放行
        if (PrincipalContext.match("/*/public/**", uri)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 3. 调用 AuthContract SPI 认证
            AuthResult authResult = authContract.authenticate(request);

            if (authResult == null) {
                ResponseHelper.responseError(response, HttpStatus.UNAUTHORIZED, "token 不存在!");
                return;
            }

            // 4. 缓存到上下文
            PrincipalContext.cache(request, authResult.getPrincipal(), authResult.getSession());

            // 5. 放行
            chain.doFilter(request, response);
        } catch (AuthException e) {
            log.warn("认证失败: {} - {}", e.getErrorType(), e.getMessage());
            ResponseHelper.responseError(response, HttpStatus.UNAUTHORIZED, e.getMessage());
        } finally {
            PrincipalContext.clear();
        }
    }
}
```

### 6.7 IamContractAutoConfig

```java
@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.contract.defaults"})
@ConditionalOnProperty(prefix = "sh.iam.contract", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamContractAutoConfig {

    @Bean @ConditionalOnMissingBean
    public AuthContract authContract() { return new DefaultAuthContract(); }

    @Bean @ConditionalOnMissingBean
    public AuthzContract authzContract() { return new DefaultAuthzContract(); }

    @Bean @ConditionalOnMissingBean
    public AkSignContract akSignContract() { return new DefaultAkSignContract(); }

    @Bean @ConditionalOnMissingBean
    public SsoFacadeContract ssoFacadeContract() { return new DefaultSsoFacadeContract(); }
}
```

替换机制: 业务方只需声明 `@Component AuthContract` 实现，`@ConditionalOnMissingBean` 自动阻止默认 Bean 注册。

### 6.8 PrincipalContext 完整实现

```java
package com.wkclz.iam.contract.context;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;

public final class PrincipalContext {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final ThreadLocal<Principal> PRINCIPAL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Session> SESSION_HOLDER = new ThreadLocal<>();

    public static void cache(HttpServletRequest request, Principal principal, Session session) {
        request.setAttribute("contractPrincipal", principal);
        request.setAttribute("contractSession", session);
        PRINCIPAL_HOLDER.set(principal);
        SESSION_HOLDER.set(session);
    }

    public static void clear() {
        PRINCIPAL_HOLDER.remove();
        SESSION_HOLDER.remove();
    }

    public static Principal getPrincipal() {
        Principal p = PRINCIPAL_HOLDER.get();
        if (p != null) return p;
        HttpServletRequest request = getRequest();
        if (request != null) {
            return (Principal) request.getAttribute("contractPrincipal");
        }
        return null;
    }

    public static Session getSession() {
        Session s = SESSION_HOLDER.get();
        if (s != null) return s;
        HttpServletRequest request = getRequest();
        if (request != null) {
            return (Session) request.getAttribute("contractSession");
        }
        return null;
    }

    public static String getUserCode() {
        Principal p = getPrincipal();
        return p != null ? p.getUserCode() : null;
    }

    public static String getUsername() {
        Principal p = getPrincipal();
        return p != null ? p.getUsername() : null;
    }

    public static String getNickname() {
        Principal p = getPrincipal();
        return p != null ? p.getNickname() : null;
    }

    public static String getTenantCode() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            String tc = request.getHeader("tenant-code");
            if (StringUtils.isNotBlank(tc)) return tc;
        }
        return null;
    }

    public static String getAppCode() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("app-code") : null;
    }

    public static String getToken() {
        HttpServletRequest request = getRequest();
        if (request == null) return null;
        String token = request.getHeader("Authorization");
        if (StringUtils.isBlank(token)) {
            token = request.getHeader("token");
        }
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }

    public static String getAuthIdentifier() {
        Session s = getSession();
        return s != null ? s.getAuthIdentifier() : null;
    }

    public static boolean match(String pattern, String uri) {
        return PATH_MATCHER.match(pattern, uri);
    }

    /**
     * 通过 Spring 的 RequestContextHolder 获取 HttpServletRequest
     * 不再依赖 sh-web 的 RequestHelper，由 spring-boot-starter-web provided scope 提供类型
     */
    private static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return null;
        }
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) requestAttributes;
        return servletRequestAttributes.getRequest();
    }
}
```

## 7. 配置项

**配置前缀约定**（对齐现有 iam-sdk 约定）：
- 自动配置开关：`sh.iam.contract.enabled`（@ConditionalOnProperty）
- 配置值：`iam.contract.*`（@Value 注入）

```yaml
sh:
  iam:
    contract:
      enabled: true                          # 是否启用契约层自动配置（默认 true）

iam:
  contract:
    auth-filter-enabled: true                # 是否注册 DefaultAuthFilter（默认 true）
    public-path-pattern: "/*/public/**"      # 公开路径匹配模式
    app-id:                                  # AK 签名 appId
    app-secret:                              # AK 签名 appSecret（RSA 私钥）
    public-key:                              # AK 验签 publicKey（RSA 公钥）
    server-url:                              # SSO 服务端地址
    jwt-secret-key:                          # JWT 密钥（供实现层使用）
```

**ContractSettings**（api 模块，静态配置持有器）:

```java
package com.wkclz.iam.contract.config;

/**
 * 契约层静态配置持有器
 * 由 IamContractAutoConfig 在启动时通过 @PostConstruct 初始化
 * 供契约接口的 default 方法（如 AkSignContract.sign()）访问配置
 */
public final class ContractSettings {

    private static String appId;
    private static String appSecret;
    private static String publicKey;
    private static String serverUrl;
    private static String jwtSecretKey;

    // getter/setter（setter 仅由 AutoConfig 调用）
    public static String getAppId() { return appId; }
    public static void setAppId(String appId) { ContractSettings.appId = appId; }
    public static String getAppSecret() { return appSecret; }
    public static void setAppSecret(String appSecret) { ContractSettings.appSecret = appSecret; }
    public static String getPublicKey() { return publicKey; }
    public static void setPublicKey(String publicKey) { ContractSettings.publicKey = publicKey; }
    public static String getServerUrl() { return serverUrl; }
    public static void setServerUrl(String serverUrl) { ContractSettings.serverUrl = serverUrl; }
    public static String getJwtSecretKey() { return jwtSecretKey; }
    public static void setJwtSecretKey(String jwtSecretKey) { ContractSettings.jwtSecretKey = jwtSecretKey; }
}
```

**IamContractAutoConfig 中的初始化**（补充）:

```java
@Autowired
private ContractConfig contractConfig;  // @ConfigurationProperties Bean

@PostConstruct
public void initContractSettings() {
    ContractSettings.setAppId(contractConfig.getAppId());
    ContractSettings.setAppSecret(contractConfig.getAppSecret());
    ContractSettings.setPublicKey(contractConfig.getPublicKey());
    ContractSettings.setServerUrl(contractConfig.getServerUrl());
    ContractSettings.setJwtSecretKey(contractConfig.getJwtSecretKey());
}
```

**设计说明**:
- `ContractConfig`（defaults 模块）是 Spring `@ConfigurationProperties` Bean，绑定 `iam.contract.*` 配置
- `ContractSettings`（api 模块）是纯静态持有器，供接口 default 方法访问
- 两者通过 `@PostConstruct` 桥接，解决 default 方法无法访问 Spring 上下文的问题

## 8. AutoConfiguration 注册

```
# sh-iam-contract/iam-contract-default/src/main/resources/META-INF/spring/
org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.wkclz.iam.contract.defaults.config.IamContractAutoConfig
```

## 9. iam-sdk 实现要求

> 以下为 iam-sdk 重构为契约层实现的详细要求，供后续实现任务参考。

### 9.1 总体要求

| 项目 | 要求 |
|------|------|
| 依赖 | `iam-sdk` 依赖 `iam-contract-api`，实现四个契约接口 |
| 替换机制 | `@Component` 注册实现 Bean，`@ConditionalOnMissingBean` 自动阻止默认 |
| 过滤器 | 移除现有 `IamAuthFilter`，由 `DefaultAuthFilter` + `AuthContract` SPI 驱动 |
| 向后兼容 | 现有 `SessionHelper` / `UserContext` 标记 `@Deprecated`，内部委托 `PrincipalContext` |
| 配置兼容 | `iam.sdk.*` 配置项继续生效，映射到 `ContractConfig` |

### 9.2 IamAuthContractImpl

**对应现有**: `IamAuthFilter` + `JwtUtil` + `IamSsoService.tokenCheck`

- `authenticate()`: 提取 token → JWT 验证 → 解析 UserJwt → tokenCheck → 映射 Principal/Session → 返回 AuthResult
- `checkToken()`: 委托 `IamSsoService.tokenCheck()` → 映射 Session
- 映射: UserJwt → Principal（userCode/username/nickname/avatar），UserSession → Session（userCode/authType/authIdentifier）

### 9.3 IamAuthzContractImpl

**对应现有**: 散在 `iam-admin` 各 Service 中

- `listTenants()`: 查询用户关联租户 → 映射 IamTenant → Tenant
- `listApps()`: 查询用户在租户下可访问应用 → 映射 IamApp → App
- `getMenuTree()`: 复用 SsoResourceService 逻辑 → 映射 IamMenu → Menu，构建树
- `canAccessApi()`: 用户角色 → 角色 API 关联 → 匹配 uri+method
- `listFieldPermissions()`: 查询 IamMenuField + IamApiField → 映射 FieldPermission
- `filterFields()`: 基于字段权限过滤
- `getDataDimensions()`: 查询 IamRoleData → 映射 DataDimension

### 9.4 IamAkSignContractImpl

**对应现有**: `AkSignHelper`

- `sign()`: 委托 `AkSignHelper.sign()`
- `verifySign()`: 委托 `AkSignHelper.verifySign()`（含 Redis nonce 校验）

### 9.5 IamSsoFacadeContractImpl

**对应现有**: `SsoFacadeImpl`

- `login()`: 映射 contract.SessionCreateReq → sdk.SessionCreateReq → 委托 SsoFacadeImpl.login()
- `saveLog()`: 映射 contract.RequestLog → sdk.RequestLog → 委托 SsoFacadeImpl.saveLog()
- `logout()`: 委托 SsoFacadeImpl.logout()

### 9.6 兼容层

- `SessionHelper`: 标记 `@Deprecated`，内部委托 PrincipalContext
- `UserContext.setUserInfo()`: 内部委托 PrincipalContext.cache()

### 9.7 迁移步骤

```
阶段 1: iam-sdk 引入 iam-contract-api 依赖
阶段 2: 实现四个契约接口
阶段 3: SessionHelper → PrincipalContext 兼容层
阶段 4: 移除 IamAuthFilter（DefaultAuthFilter + IamAuthContractImpl 自动接管）
阶段 5: 验证 & 清理
```

## 10. 模块依赖变化对比

**重构前**: 业务模块 → iam-sdk（重依赖：JWT + Redis + HTTP Client + AK 签名）

**重构后**:
- 轻量业务模块 → iam-contract-api（零重依赖，仅 sh-*）
- 需要认证的模块 → iam-contract-default（自动注册过滤器 + 默认实现）
- IAM 体系内模块 → iam-sdk（实现契约，提供 JWT/Redis/AK 全能力）

## 11. 契约依赖关系图

```
DefaultAuthFilter ──calls──> AuthContract.authenticate()
                          │
                          ├──> PrincipalContext.cache()  (写)
                          │
                          ▼
业务代码 ──────────────> PrincipalContext.getPrincipal()  (读)
              │
              ├──> AuthzContract.canAccessApi()    (接口鉴权)
              ├──> AuthzContract.getMenuTree()     (菜单查询)
              └──> AuthzContract.filterFields()    (字段裁剪)

服务间 RPC ──> AkSignContract.sign() / verifySign()

客户端应用 ──> SsoFacadeContract.login() / logout() / saveLog()
```
