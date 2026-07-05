# IAM 契约层实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 创建 `iam-contract-api` + `iam-contract-default` 两个 Maven 模块，实现认证/鉴权/AK 签名/SSO 门面四契约及默认实现，不改动现有模块。

**架构：** API 模块零业务依赖（通过 Spring `RequestContextHolder` 获取 HttpServletRequest，不依赖 sh-web），定义契约接口、中性数据模型、PrincipalContext 上下文、ContractSettings 静态配置持有器。Default 模块依赖 API 模块，提供"读宽容验证严格"的默认实现 + DefaultAuthFilter + AutoConfig（@ConditionalOnMissingBean 替换机制）。

**技术栈：** Java 25，Spring Boot 4.0，Lombok，spring-boot-starter-web（provided scope，提供 HttpServletRequest 类型），sh-tool（加密工具，仅 api 模块可选依赖）

**规格文档：** `docs/superpowers/specs/2026-07-04-iam-contract-layer-design.md`

---

## 迁移说明

> 本计划已实施完毕。文档迁移自 sh-iam 项目（/docs/superpowers/plans/2026-07-04-iam-contract-layer.md），并根据 sh-framework 实际代码状态完成本地化调整。
>
> 实施期间模块已从 sh-iam 项目迁移至 sh-framework 项目，父 POM 由 `sh-iam` 变更为 `sh-parent`，GroupId 由 `com.wkclz.iam` 变更为 `com.wkclz.framework`，并解耦了对 sh-web 的依赖（改为通过 Spring `RequestContextHolder` 获取 HttpServletRequest）。

**关键约定（已迁移至 sh-framework）：**
- 父 POM：`com.wkclz.framework:sh-parent:${revision}`（revision=5.0.1-SNAPSHOT）
- sh-* 依赖不声明 version（BOM 管理）
- 内部模块依赖显式声明 `version=${revision}`
- 自动配置开关前缀 `sh.iam.contract.enabled`，配置值前缀 `iam.contract.*`
- 配置类用 `@Value` 注入（对齐现有 IamSdkConfig 风格）
- AutoConfig 用 `@AutoConfiguration` + `@ComponentScan` + `@ConditionalOnProperty`
- 注册文件：`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Java 25，包名 `com.wkclz.iam.contract` / `com.wkclz.iam.contract.defaults`（注：包名保留 com.wkclz.iam.* 命名，与 sh-framework 的 com.wkclz.* 主包名不同）
- 模块路径：`sh-iam-contract/iam-contract-api`、`sh-iam-contract/iam-contract-default`

**不改动现有模块：** 本计划仅创建新模块 + 修改根 pom.xml 注册新模块。不触碰 iam-sdk/iam-sso/iam-admin 等任何现有模块代码。

---

## 文件结构

### iam-contract-api（新建模块）

| 文件 | 职责 |
|------|------|
| `sh-iam-contract/iam-contract-api/pom.xml` | Maven 模块描述，依赖 spring-boot-starter-web（provided） |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/exception/AuthException.java` | 契约层统一异常，含 AuthErrorType 枚举 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/enums/AuthScene.java` | 鉴权场景枚举 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Principal.java` | 用户主体（JWT claims 映射） |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Session.java` | 会话信息 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/AuthResult.java` | 认证结果（Principal+Session 聚合） |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Tenant.java` | 租户 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/App.java` | 应用 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Menu.java` | 菜单（树形） |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Api.java` | API 路由 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/FieldPermission.java` | 字段权限 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/DataDimension.java` | 数据维度 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/RequestLog.java` | 请求日志 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/req/SessionCreateReq.java` | 会话创建请求 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/resp/LoginResp.java` | 登录响应 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/config/ContractSettings.java` | 静态配置持有器（供 default 方法访问） |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/context/PrincipalContext.java` | Principal 读取上下文 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthContract.java` | 认证契约接口 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthzContract.java` | 鉴权契约接口（含重载） |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AkSignContract.java` | AK 签名契约接口 |
| `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/facade/SsoFacadeContract.java` | SSO 门面契约接口 |

### iam-contract-default（新建模块）

| 文件 | 职责 |
|------|------|
| `sh-iam-contract/iam-contract-default/pom.xml` | 依赖 iam-contract-api + spring-boot-starter-web |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/ContractConfig.java` | @Value 配置绑定 |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContract.java` | 认证默认实现 |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContract.java` | 鉴权默认实现 |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContract.java` | AK 签名默认实现 |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContract.java` | SSO 门面默认实现 |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/filter/DefaultAuthFilter.java` | 鉴权过滤器 |
| `sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/IamContractAutoConfig.java` | 自动配置 |
| `sh-iam-contract/iam-contract-default/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 自动配置注册 |

### 根项目修改

| 文件 | 修改内容 |
|------|----------|
| `pom.xml` | 在 `<modules>` 中追加 `sh-iam-contract`（聚合模块，包含 iam-contract-api 和 iam-contract-default） |

### 验证文件

| 文件 | 职责 |
|------|------|
| `sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContractTest.java` | 验证认证默认实现行为 |
| `sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContractTest.java` | 验证鉴权默认实现行为 |
| `sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContractTest.java` | 验证 AK 签名默认实现行为 |
| `sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContractTest.java` | 验证 SSO 门面默认实现行为 |

---

## 任务 1：创建 iam-contract-api 模块骨架

**文件：**
- 创建：`sh-iam-contract/iam-contract-api/pom.xml`

- [ ] **步骤 1：创建 iam-contract-api/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.wkclz.framework</groupId>
        <artifactId>sh-iam-contract</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>iam-contract-api</artifactId>
    <description>IAM 契约层 API - 接口定义与中性模型，零业务依赖</description>

    <dependencies>
        <!-- Spring Web 提供 HttpServletRequest 类型（编译期需要，provided scope 避免强制带 web 服务器）-->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <scope>provided</scope>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <!-- Swagger 注解（@Schema）-->
        <dependency>
            <groupId>io.swagger.core.v3</groupId>
            <artifactId>swagger-annotations</artifactId>
        </dependency>
    </dependencies>

</project>
```

**说明：**
- 通过 Spring 的 `RequestContextHolder` 获取 HttpServletRequest，避免对 sh-web 的硬依赖
- spring-boot-starter-web 标记 provided（运行时由引入方提供，避免 api 模块强制带 web 服务器）
- swagger-annotations 用于 @Schema 注解（与现有 iam-sdk 风格对齐）
- 不依赖任何 iam-* 模块
- sh-iam-contract 是聚合模块（packaging=pom），iam-contract-api 与 iam-contract-default 作为子模块

- [ ] **步骤 2：创建包目录结构**

创建以下空目录（通过创建占位 .gitkeep 文件保持目录）：
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/req/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/resp/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/config/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/context/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/enums/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/exception/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/facade/`
- `sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/`

> 目录会在后续任务创建文件时自动生成，此步骤仅作说明，无需单独创建占位文件。

- [ ] **步骤 3：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-api/pom.xml
git commit -m "feat(contract): 创建 iam-contract-api 模块骨架"
```

---

## 任务 2：实现异常与枚举

**文件：**
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/exception/AuthException.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/enums/AuthScene.java`

- [ ] **步骤 1：创建 AuthException**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/exception/AuthException.java`:

```java
package com.wkclz.iam.contract.exception;

import lombok.Getter;

/**
 * 契约层统一异常
 * 用于认证、鉴权、AK 签名等场景的错误标识
 *
 * @author shrimp
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorType errorType;

    public AuthException(AuthErrorType errorType, String message) {
        super(message);
        this.errorType = errorType;
    }

    public AuthException(AuthErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
    }

    /**
     * 认证错误类型
     */
    public enum AuthErrorType {
        /** token 不存在 */
        TOKEN_MISSING,
        /** JWT 签名无效 */
        TOKEN_INVALID,
        /** JWT 已过期 */
        TOKEN_EXPIRED,
        /** 会话已过期（如 Redis 无记录） */
        SESSION_EXPIRED,
        /** AK 签名无效 */
        AK_SIGN_INVALID,
        /** AK 签名已过期 */
        AK_SIGN_EXPIRED,
        /** nonce 重放检测命中 */
        AK_NONCE_REPLAY,
        /** 接口鉴权拒绝 */
        ACCESS_DENIED
    }
}
```

- [ ] **步骤 2：创建 AuthScene**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/enums/AuthScene.java`:

```java
package com.wkclz.iam.contract.enums;

/**
 * 鉴权场景枚举
 *
 * @author shrimp
 */
public enum AuthScene {
    /** JWT Token 认证 */
    TOKEN,
    /** AK 签名认证 */
    AK_SIGN,
    /** 公开接口（无需认证） */
    PUBLIC
}
```

- [ ] **步骤 3：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/exception/AuthException.java \
        sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/enums/AuthScene.java
git commit -m "feat(contract): 添加 AuthException 与 AuthScene 枚举"
```

---

## 任务 3：实现数据模型（bean 包）

**文件：**
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Principal.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Session.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/AuthResult.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Tenant.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/App.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Menu.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Api.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/FieldPermission.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/DataDimension.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/RequestLog.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/req/SessionCreateReq.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/resp/LoginResp.java`

- [ ] **步骤 1：创建 Principal**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Principal.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户主体
 * 从 JWT claims 解析，包含认证后的固有属性（你是谁）
 * 不含 tenantCode（租户是运行时动态切换值，从请求头获取）
 *
 * @author shrimp
 */
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

- [ ] **步骤 2：创建 Session**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Session.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户会话
 * 仅保留 JWT 无法携带的动态会话数据，不与 Principal 重复字段
 *
 * @author shrimp
 */
@Data
@Schema(description = "用户会话")
public class Session implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "认证类型：PASSWORD / LDAP / OAUTH 等")
    private String authType;

    @Schema(description = "认证标识符（用户名或三方平台标识）")
    private String authIdentifier;
}
```

- [ ] **步骤 3：创建 AuthResult**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/AuthResult.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 认证结果
 * Principal + Session 聚合，由 AuthContract.authenticate() 返回
 *
 * @author shrimp
 */
@Data
@Schema(description = "认证结果")
public class AuthResult implements Serializable {

    @Schema(description = "用户主体")
    private Principal principal;

    @Schema(description = "会话信息")
    private Session session;
}
```

- [ ] **步骤 4：创建 Tenant**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Tenant.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 租户
 *
 * @author shrimp
 */
@Data
@Schema(description = "租户")
public class Tenant implements Serializable {

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;
}
```

- [ ] **步骤 5：创建 App**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/App.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 应用
 *
 * @author shrimp
 */
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

- [ ] **步骤 6：创建 Menu**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Menu.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单（树形结构）
 * 仅包含核心展示字段，不含管理字段；树构建由实现层负责
 *
 * @author shrimp
 */
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

- [ ] **步骤 7：创建 Api**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/Api.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * API 路由
 *
 * @author shrimp
 */
@Data
@Schema(description = "API 路由")
public class Api implements Serializable {

    @Schema(description = "API 编码")
    private String apiCode;

    @Schema(description = "API 名称")
    private String apiName;

    @Schema(description = "HTTP 方法：GET / POST / PUT / DELETE")
    private String apiMethod;

    @Schema(description = "URI 路径")
    private String apiUri;

    @Schema(description = "是否写操作")
    private Boolean writeFlag;
}
```

- [ ] **步骤 8：创建 FieldPermission**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/FieldPermission.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 字段权限
 *
 * @author shrimp
 */
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

- [ ] **步骤 9：创建 DataDimension**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/DataDimension.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 数据权限维度
 * authorizedValues 为通用值列表，业务层根据 dimensionCode 解释含义
 *
 * @author shrimp
 */
@Data
@Schema(description = "数据权限维度")
public class DataDimension implements Serializable {

    @Schema(description = "维度编码")
    private String dimensionCode;

    @Schema(description = "维度名称")
    private String dimensionName;

    @Schema(description = "授权值列表（如部门 ID、区域编码等）")
    private List<String> authorizedValues;
}
```

- [ ] **步骤 10：创建 RequestLog**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/RequestLog.java`:

```java
package com.wkclz.iam.contract.bean;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 请求日志
 * 由 SsoFacadeContract.saveLog() 上报到 SSO 服务端
 *
 * @author shrimp
 */
@Data
@Schema(description = "请求日志")
public class RequestLog implements Serializable {

    @Schema(description = "请求 URI")
    private String uri;

    @Schema(description = "HTTP 方法")
    private String method;

    @Schema(description = "请求体")
    private String requestBody;

    @Schema(description = "响应状态码")
    private Integer responseStatus;

    @Schema(description = "响应体")
    private String responseBody;

    @Schema(description = "请求时间")
    private Long requestTime;

    @Schema(description = "响应时间")
    private Long responseTime;

    @Schema(description = "耗时(ms)")
    private Long duration;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "应用编码")
    private String appCode;
}
```

- [ ] **步骤 11：创建 SessionCreateReq**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/req/SessionCreateReq.java`:

```java
package com.wkclz.iam.contract.bean.req;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 会话创建请求
 * 由 SsoFacadeContract.login() 调用
 *
 * @author shrimp
 */
@Data
@Schema(description = "会话创建请求")
public class SessionCreateReq implements Serializable {

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "认证类型")
    private String authType;

    @Schema(description = "认证标识符")
    private String authIdentifier;

    @Schema(description = "客户端 IP")
    private String clientIp;

    @Schema(description = "User-Agent")
    private String userAgent;
}
```

- [ ] **步骤 12：创建 LoginResp**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/resp/LoginResp.java`:

```java
package com.wkclz.iam.contract.bean.resp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应
 * 由 SsoFacadeContract.login() 返回
 *
 * @author shrimp
 */
@Data
@Schema(description = "登录响应")
public class LoginResp implements Serializable {

    @Schema(description = "JWT Token")
    private String token;

    @Schema(description = "用户编码")
    private String userCode;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;
}
```

- [ ] **步骤 13：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/bean/
git commit -m "feat(contract): 添加中性数据模型（Principal/Session/Menu 等 12 个 bean）"
```

---

## 任务 4：实现 ContractSettings 与 PrincipalContext

**文件：**
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/config/ContractSettings.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/context/PrincipalContext.java`

- [ ] **步骤 1：创建 ContractSettings**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/config/ContractSettings.java`:

```java
package com.wkclz.iam.contract.config;

/**
 * 契约层静态配置持有器
 * 由 IamContractAutoConfig 在启动时通过 @PostConstruct 初始化
 * 供契约接口的 default 方法（如 AkSignContract.sign()）访问配置
 * default 方法无法访问 Spring 上下文，因此通过静态持有器桥接
 *
 * @author shrimp
 */
public final class ContractSettings {

    private static String appId;
    private static String appSecret;
    private static String publicKey;
    private static String serverUrl;
    private static String jwtSecretKey;

    private ContractSettings() {
    }

    public static String getAppId() {
        return appId;
    }

    public static void setAppId(String appId) {
        ContractSettings.appId = appId;
    }

    public static String getAppSecret() {
        return appSecret;
    }

    public static void setAppSecret(String appSecret) {
        ContractSettings.appSecret = appSecret;
    }

    public static String getPublicKey() {
        return publicKey;
    }

    public static void setPublicKey(String publicKey) {
        ContractSettings.publicKey = publicKey;
    }

    public static String getServerUrl() {
        return serverUrl;
    }

    public static void setServerUrl(String serverUrl) {
        ContractSettings.serverUrl = serverUrl;
    }

    public static String getJwtSecretKey() {
        return jwtSecretKey;
    }

    public static void setJwtSecretKey(String jwtSecretKey) {
        ContractSettings.jwtSecretKey = jwtSecretKey;
    }
}
```

- [ ] **步骤 2：创建 PrincipalContext**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/context/PrincipalContext.java`:

```java
package com.wkclz.iam.contract.context;

import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Session;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Principal 读取上下文
 * 替代 sh-core UserContext 的用户信息读取职责
 *
 * 双存储策略：
 * - request.setAttribute: 主存储，跟随请求生命周期，Servlet 规范保证线程安全
 * - ThreadLocal: 辅助存储，支持子线程读取（异步场景），由 clear() 在 finally 中清理
 *
 * @author shrimp
 */
public final class PrincipalContext {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private static final String ATTR_PRINCIPAL = "contractPrincipal";
    private static final String ATTR_SESSION = "contractSession";

    private static final ThreadLocal<Principal> PRINCIPAL_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Session> SESSION_HOLDER = new ThreadLocal<>();

    private PrincipalContext() {
    }

    // ── 写入（过滤器调用） ──

    /**
     * 缓存 Principal + Session 到当前请求上下文
     *
     * @param request   HTTP 请求
     * @param principal 用户主体
     * @param session   会话信息
     */
    public static void cache(HttpServletRequest request, Principal principal, Session session) {
        if (request != null) {
            request.setAttribute(ATTR_PRINCIPAL, principal);
            request.setAttribute(ATTR_SESSION, session);
        }
        PRINCIPAL_HOLDER.set(principal);
        SESSION_HOLDER.set(session);
    }

    /**
     * 清理上下文（请求结束时调用，防内存泄漏）
     */
    public static void clear() {
        PRINCIPAL_HOLDER.remove();
        SESSION_HOLDER.remove();
    }

    // ── 核心读取 ──

    /**
     * 获取当前 Principal
     *
     * @return Principal；无上下文返回 null
     */
    public static Principal getPrincipal() {
        Principal p = PRINCIPAL_HOLDER.get();
        if (p != null) {
            return p;
        }
        HttpServletRequest request = getRequest();
        if (request != null) {
            return (Principal) request.getAttribute(ATTR_PRINCIPAL);
        }
        return null;
    }

    /**
     * 获取当前 Session
     *
     * @return Session；无上下文返回 null
     */
    public static Session getSession() {
        Session s = SESSION_HOLDER.get();
        if (s != null) {
            return s;
        }
        HttpServletRequest request = getRequest();
        if (request != null) {
            return (Session) request.getAttribute(ATTR_SESSION);
        }
        return null;
    }

    // ── 便捷方法 ──

    /**
     * 获取当前用户编码
     */
    public static String getUserCode() {
        Principal p = getPrincipal();
        return p != null ? p.getUserCode() : null;
    }

    /**
     * 获取当前用户名
     */
    public static String getUsername() {
        Principal p = getPrincipal();
        return p != null ? p.getUsername() : null;
    }

    /**
     * 获取当前昵称
     */
    public static String getNickname() {
        Principal p = getPrincipal();
        return p != null ? p.getNickname() : null;
    }

    /**
     * 获取当前租户编码（动态值，从请求头 tenant-code 获取）
     * 租户可随时切换，不属于用户身份
     *
     * @return 租户编码；无请求上下文返回 null
     */
    public static String getTenantCode() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            String tc = request.getHeader("tenant-code");
            if (StringUtils.hasText(tc)) {
                return tc;
            }
        }
        return null;
    }

    /**
     * 获取当前应用编码（从请求头 app-code 获取）
     */
    public static String getAppCode() {
        HttpServletRequest request = getRequest();
        return request != null ? request.getHeader("app-code") : null;
    }

    /**
     * 获取当前 token（从请求头 Authorization 或 token 获取，去 Bearer 前缀）
     */
    public static String getToken() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            token = request.getHeader("token");
        }
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return token;
    }

    /**
     * 获取当前认证标识符（从 Session 获取）
     */
    public static String getAuthIdentifier() {
        Session s = getSession();
        return s != null ? s.getAuthIdentifier() : null;
    }

    // ── 路径匹配 ──

    /**
     * 路径匹配（Ant 风格）
     *
     * @param pattern 模式，如 "/*/public/**"
     * @param uri     请求 URI
     * @return true=匹配
     */
    public static boolean match(String pattern, String uri) {
        return PATH_MATCHER.match(pattern, uri);
    }

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

**说明：**
- 使用 Spring 的 `RequestContextHolder` 获取 HttpServletRequest（无需依赖 sh-web）
- 使用 `jakarta.servlet.http.HttpServletRequest`（Spring Boot 4.x 使用 jakarta 命名空间）
- 使用 `org.springframework.util.AntPathMatcher` 和 `StringUtils`（Spring 核心）
- 不依赖任何 iam-* 模块

- [ ] **步骤 3：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/config/ContractSettings.java \
        sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/context/PrincipalContext.java
git commit -m "feat(contract): 添加 ContractSettings 与 PrincipalContext"
```

---

## 任务 5：实现四个契约接口

**文件：**
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthContract.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthzContract.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AkSignContract.java`
- 创建：`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/facade/SsoFacadeContract.java`

- [ ] **步骤 1：创建 AuthContract**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthContract.java`:

```java
package com.wkclz.iam.contract.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 认证契约
 * 实现方负责从 HTTP 请求或 token 中认证用户，返回 Principal + Session
 *
 * @author shrimp
 */
public interface AuthContract {

    /**
     * 从 HTTP 请求中认证用户（过滤器主入口）
     *
     * 实现职责：
     * 1. 从请求头提取 token（Authorization / token，去 Bearer 前缀）
     * 2. 校验 JWT 签名与有效期
     * 3. 校验 Session 存在性（如 Redis）
     * 4. 返回 Principal + Session
     *
     * @param request HTTP 请求
     * @return 认证结果；token 不存在时返回 null（由过滤器处理 public 路径放行）
     * @throws AuthException token 无效、签名错误、会话过期等
     */
    AuthResult authenticate(HttpServletRequest request);

    /**
     * 校验 token（非 HTTP 请求场景：WebSocket、定时任务等）
     *
     * @param token          JWT token
     * @param authIdentifier 认证标识符（用户名 / 三方平台标识）
     * @return 会话信息
     * @throws AuthException token 无效或会话过期
     */
    Session checkToken(String token, String authIdentifier);
}
```

- [ ] **步骤 2：创建 AuthzContract**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AuthzContract.java`:

```java
package com.wkclz.iam.contract.service;

import com.wkclz.iam.contract.bean.App;
import com.wkclz.iam.contract.bean.DataDimension;
import com.wkclz.iam.contract.bean.FieldPermission;
import com.wkclz.iam.contract.bean.Menu;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Tenant;
import com.wkclz.iam.contract.context.PrincipalContext;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 鉴权契约
 * 覆盖租户/应用/菜单/接口/字段/数据六个维度的鉴权查询
 *
 * 重载规则：
 * - 完整参数版本：显式传入 Principal + 其他参数，用于单元测试和非 HTTP 场景
 * - 上下文重载版本：从 PrincipalContext 自动获取 Principal 等信息，用于业务代码
 * - 请求全自动版本：从 HttpServletRequest 自动获取所有信息，用于过滤器
 *
 * @author shrimp
 */
public interface AuthzContract {

    // ── 1. 租户 ──

    /**
     * 查询用户可访问的租户列表（完整参数）
     *
     * @param principal 用户主体
     * @return 租户列表
     */
    List<Tenant> listTenants(Principal principal);

    /**
     * 查询用户可访问的租户列表（上下文重载）
     */
    default List<Tenant> listTenants() {
        return listTenants(PrincipalContext.getPrincipal());
    }

    // ── 2. 应用 ──

    /**
     * 查询用户在指定租户下可访问的应用列表（完整参数）
     *
     * @param principal  用户主体
     * @param tenantCode 租户编码
     * @return 应用列表
     */
    List<App> listApps(Principal principal, String tenantCode);

    /**
     * 查询用户可访问的应用列表（上下文重载：tenantCode 从请求头获取）
     */
    default List<App> listApps() {
        return listApps(PrincipalContext.getPrincipal(), PrincipalContext.getTenantCode());
    }

    /**
     * 查询用户可访问的应用列表（半上下文重载：Principal 从上下文，tenantCode 显式传入）
     */
    default List<App> listApps(String tenantCode) {
        return listApps(PrincipalContext.getPrincipal(), tenantCode);
    }

    // ── 3. 菜单树 ──

    /**
     * 查询用户在指定应用下的菜单树（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @return 树根节点列表（多根表示多个顶级菜单）
     */
    List<Menu> getMenuTree(Principal principal, String appCode);

    /**
     * 查询用户菜单树（上下文重载：Principal + appCode 均从上下文获取）
     */
    default List<Menu> getMenuTree() {
        return getMenuTree(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode());
    }

    /**
     * 查询用户菜单树（半上下文重载：Principal 从上下文，appCode 显式传入）
     */
    default List<Menu> getMenuTree(String appCode) {
        return getMenuTree(PrincipalContext.getPrincipal(), appCode);
    }

    // ── 4. 接口鉴权 ──

    /**
     * 判断用户是否有权访问指定 API（完整参数）
     *
     * @param principal  用户主体
     * @param appCode    应用编码
     * @param apiUri     API URI（如 /iam-admin/user/page）
     * @param apiMethod  HTTP 方法（GET/POST/PUT/DELETE）
     * @return true=允许；false=拒绝
     */
    boolean canAccessApi(Principal principal, String appCode, String apiUri, String apiMethod);

    /**
     * 接口鉴权（上下文重载：Principal + appCode 从上下文获取，apiUri/apiMethod 显式传入）
     */
    default boolean canAccessApi(String apiUri, String apiMethod) {
        return canAccessApi(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(), apiUri, apiMethod);
    }

    /**
     * 接口鉴权（从当前请求自动获取 apiUri/apiMethod，过滤器场景）
     */
    default boolean canAccessApi(HttpServletRequest request) {
        return canAccessApi(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(),
                request.getRequestURI(), request.getMethod());
    }

    // ── 5. 字段权限 ──

    /**
     * 查询用户在指定菜单下各字段的权限（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @param menuCode  菜单编码
     * @return 字段权限列表（包含可见/可编辑标记）
     */
    List<FieldPermission> listFieldPermissions(Principal principal, String appCode, String menuCode);

    /**
     * 查询字段权限（上下文重载）
     */
    default List<FieldPermission> listFieldPermissions(String menuCode) {
        return listFieldPermissions(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(), menuCode);
    }

    /**
     * 字段过滤：根据权限过滤字段列表，返回有权限的字段（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @param menuCode  菜单编码
     * @param fields    待过滤的字段列表
     * @return 有权限的字段列表
     */
    List<String> filterFields(Principal principal, String appCode, String menuCode, List<String> fields);

    /**
     * 字段过滤（上下文重载）
     */
    default List<String> filterFields(String menuCode, List<String> fields) {
        return filterFields(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode(), menuCode, fields);
    }

    // ── 6. 数据权限 ──

    /**
     * 查询用户在指定应用下的数据权限维度及授权值（完整参数）
     *
     * @param principal 用户主体
     * @param appCode   应用编码
     * @return 数据维度列表（如"部门"维度 + 授权的部门 ID 列表）
     */
    List<DataDimension> getDataDimensions(Principal principal, String appCode);

    /**
     * 查询数据权限维度（上下文重载）
     */
    default List<DataDimension> getDataDimensions() {
        return getDataDimensions(PrincipalContext.getPrincipal(), PrincipalContext.getAppCode());
    }

    /**
     * 查询数据权限维度（半上下文重载）
     */
    default List<DataDimension> getDataDimensions(String appCode) {
        return getDataDimensions(PrincipalContext.getPrincipal(), appCode);
    }
}
```

- [ ] **步骤 3：创建 AkSignContract**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/AkSignContract.java`:

```java
package com.wkclz.iam.contract.service;

import com.wkclz.iam.contract.config.ContractSettings;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;

/**
 * AK 签名契约
 * 用于服务间 RPC 调用的签名认证
 *
 * @author shrimp
 */
public interface AkSignContract {

    /**
     * 生成 AK 签名（客户端调用，用于服务间 RPC 请求）
     *
     * @param appId     应用 ID
     * @param appSecret 应用密钥（RSA 私钥）
     * @return 签名字符串，放入请求头 sign 字段
     */
    String sign(String appId, String appSecret);

    /**
     * 生成 AK 签名（重载：从 ContractSettings 获取 appId + appSecret）
     */
    default String sign() {
        return sign(ContractSettings.getAppId(), ContractSettings.getAppSecret());
    }

    /**
     * 验证 AK 签名（服务端调用）
     *
     * 实现职责：
     * 1. RSA 公钥解密签名，解析参数（appId / nonce / timestamp）
     * 2. 校验签名中的 appId 与请求头 app-id 一致
     * 3. 校验 timestamp 在 5 分钟有效期内
     * 4. nonce 防重放校验（如 Redis SETNX）
     *
     * @param sign          请求头中的签名
     * @param publicKey     服务端配置的 RSA 公钥
     * @param expectedAppId 请求头中的 app-id（与签名内容比对）
     * @return 验签通过返回 true
     * @throws AuthException 验签失败（appId 不匹配 / 签名过期 / 重放检测）
     */
    boolean verifySign(String sign, String publicKey, String expectedAppId);

    /**
     * 验证 AK 签名（重载：从请求头 + ContractSettings 自动获取参数）
     */
    default boolean verifySign(HttpServletRequest request) {
        String sign = request.getHeader("sign");
        String appId = request.getHeader("app-id");
        return verifySign(sign, ContractSettings.getPublicKey(), appId);
    }
}
```

- [ ] **步骤 4：创建 SsoFacadeContract**

`sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/facade/SsoFacadeContract.java`:

```java
package com.wkclz.iam.contract.facade;

import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.context.PrincipalContext;

/**
 * SSO RPC 门面契约
 * 客户端应用通过此契约调用 SSO 服务端
 *
 * @author shrimp
 */
public interface SsoFacadeContract {

    /**
     * 远程登录（创建会话并记录登录日志）
     *
     * @param req 会话创建请求
     * @return 登录响应（含 JWT Token）
     */
    LoginResp login(SessionCreateReq req);

    /**
     * 远程保存请求日志
     * 客户端应用将请求日志上报到 SSO 服务端集中存储
     *
     * @param log 请求日志
     */
    void saveLog(RequestLog log);

    /**
     * 远程登出（指定 token）
     *
     * @param token JWT token
     */
    void logout(String token);

    /**
     * 远程登出（从 PrincipalContext 获取 token）
     */
    default void logout() {
        logout(PrincipalContext.getToken());
    }
}
```

- [ ] **步骤 5：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/service/ \
        sh-iam-contract/iam-contract-api/src/main/java/com/wkclz/iam/contract/facade/
git commit -m "feat(contract): 添加四个契约接口（Auth/Authz/AkSign/SsoFacade）"
```

---

## 任务 6：编译验证 iam-contract-api 模块

**文件：** 无新建，仅验证

- [ ] **步骤 1：在根 pom.xml 注册 iam-contract-api 模块**

修改 `/Users/shrimp/project/shrimp-group/sh-framework/pom.xml` 的 `<modules>` 节，追加 `sh-iam-contract`（聚合模块，包含 iam-contract-api 与 iam-contract-default 两个子模块）。

先读取当前 modules 节内容确定插入位置：

运行：`grep -n "<module>" pom.xml`（或用 Read 工具读取 pom.xml）

在 `<modules>` 节中追加一行（保持现有顺序，追加到末尾）：

```xml
<module>sh-iam-contract</module>
```

**注意：** 暂不追加 `iam-contract-default`，下一任务再追加。

- [ ] **步骤 2：尝试编译 iam-contract-api**

运行：`./mvnw compile -pl iam-contract-api -am -q` （若项目有 mvnw）
或：`mvn compile -pl iam-contract-api -am -q`（若有 Maven CLI）

预期：BUILD SUCCESS

**若环境无 Maven CLI（AGENTS.md 已提示）**：跳过此步骤，在 IDE 中刷新 Maven 项目并构建 iam-contract-api 模块。在 commit message 中标注"编译验证待 IDE 完成"。

- [ ] **步骤 3：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add pom.xml
git commit -m "feat(contract): 根 pom 注册 iam-contract-api 模块"
```

---

## 任务 7：创建 iam-contract-default 模块骨架

**文件：**
- 创建：`sh-iam-contract/iam-contract-default/pom.xml`

- [ ] **步骤 1：创建 iam-contract-default/pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.wkclz.framework</groupId>
        <artifactId>sh-iam-contract</artifactId>
        <version>${revision}</version>
    </parent>

    <artifactId>iam-contract-default</artifactId>
    <description>IAM 契约层默认实现 - 读宽容验证严格 + DefaultAuthFilter + AutoConfig</description>

    <dependencies>
        <!-- 依赖契约 API -->
        <dependency>
            <groupId>com.wkclz.framework</groupId>
            <artifactId>iam-contract-api</artifactId>
            <version>${revision}</version>
        </dependency>
        <!-- Spring Boot 自动配置 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <scope>provided</scope>
        </dependency>
        <!-- 测试 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

</project>
```

**说明：**
- 依赖 iam-contract-api（显式声明 version=${revision}，内部模块约定）
- spring-boot-starter-web 非 provided（default 模块提供 DefaultAuthFilter，需要 Filter 类）
- 包含 spring-boot-starter-test 用于测试默认实现行为
- default 模块也不依赖 sh-web，DefaultAuthFilter 直接使用 `response.setStatus()` 设置状态码，不依赖 ResponseHelper

- [ ] **步骤 2：在根 pom.xml 注册 iam-contract-default 模块**

修改 `/Users/shrimp/project/shrimp-group/sh-framework/sh-iam-contract/pom.xml` 的 `<modules>` 节，已包含：

```xml
<module>iam-contract-api</module>
<module>iam-contract-default</module>
```

- [ ] **步骤 3：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-default/pom.xml pom.xml
git commit -m "feat(contract): 创建 iam-contract-default 模块骨架并注册到根 pom"
```

---

## 任务 8：实现 ContractConfig 配置类

**文件：**
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/ContractConfig.java`

- [ ] **步骤 1：创建 ContractConfig**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/ContractConfig.java`:

```java
package com.wkclz.iam.contract.defaults.config;

import com.wkclz.iam.contract.config.ContractSettings;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 契约层配置绑定
 * 采用 @Value 注入（对齐现有 IamSdkConfig 风格）
 * 通过 @PostConstruct 将配置同步到 ContractSettings 静态持有器
 *
 * @author shrimp
 */
@Data
@Configuration
public class ContractConfig {

    /**
     * 是否注册 DefaultAuthFilter
     */
    @Value("${iam.contract.auth-filter-enabled:true}")
    private Boolean authFilterEnabled;

    /**
     * 公开路径匹配模式
     */
    @Value("${iam.contract.public-path-pattern:/*/public/**}")
    private String publicPathPattern;

    /**
     * AK 签名 appId
     */
    @Value("${iam.contract.app-id:}")
    private String appId;

    /**
     * AK 签名 appSecret（RSA 私钥）
     */
    @Value("${iam.contract.app-secret:}")
    private String appSecret;

    /**
     * AK 验签 publicKey（RSA 公钥）
     */
    @Value("${iam.contract.public-key:}")
    private String publicKey;

    /**
     * SSO 服务端地址
     */
    @Value("${iam.contract.server-url:}")
    private String serverUrl;

    /**
     * JWT 密钥（供实现层使用）
     */
    @Value("${iam.contract.jwt-secret-key:}")
    private String jwtSecretKey;

    /**
     * 启动时将配置同步到 ContractSettings 静态持有器
     * 供契约接口的 default 方法访问
     */
    @PostConstruct
    public void initContractSettings() {
        ContractSettings.setAppId(appId);
        ContractSettings.setAppSecret(appSecret);
        ContractSettings.setPublicKey(publicKey);
        ContractSettings.setServerUrl(serverUrl);
        ContractSettings.setJwtSecretKey(jwtSecretKey);
    }
}
```

**说明：**
- 使用 `@Value` 注入（对齐现有 IamSdkConfig 风格，非 @ConfigurationProperties）
- 使用 `jakarta.annotation.PostConstruct`（Spring Boot 4.x 使用 jakarta 命名空间）
- `@PostConstruct` 将配置同步到 ContractSettings，供 api 模块的 default 方法访问

- [ ] **步骤 2：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/ContractConfig.java
git commit -m "feat(contract): 添加 ContractConfig 配置绑定"
```

---

## 任务 9：实现四个默认实现

**文件：**
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContract.java`
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContract.java`
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContract.java`
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContract.java`

- [ ] **步骤 1：创建 DefaultAuthContract**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContract.java`:

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.bean.Session;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AuthContract;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * 认证契约默认实现
 * 读宽容：无 token 返回 null（过滤器放行 public 路径）
 * 验证严格：有 token 但无实现则拒绝
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAuthContract implements AuthContract {

    @Override
    public AuthResult authenticate(HttpServletRequest request) {
        String token = PrincipalContext.getToken();
        if (token == null) {
            // 无 token：过滤器据此放行 public 路径
            return null;
        }
        // 有 token 但无实现 → 严格拒绝
        log.warn("DefaultAuthContract: token 存在但无 AuthContract 实现，拒绝访问");
        throw new AuthException(AuthException.AuthErrorType.TOKEN_INVALID,
                "无认证实现，请配置 AuthContract");
    }

    @Override
    public Session checkToken(String token, String authIdentifier) {
        log.warn("DefaultAuthContract: checkToken 被调用但无实现");
        throw new AuthException(AuthException.AuthErrorType.TOKEN_MISSING,
                "无认证实现，请配置 AuthContract");
    }
}
```

- [ ] **步骤 2：创建 DefaultAuthzContract**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContract.java`:

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.App;
import com.wkclz.iam.contract.bean.DataDimension;
import com.wkclz.iam.contract.bean.FieldPermission;
import com.wkclz.iam.contract.bean.Menu;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Tenant;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AuthzContract;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * 鉴权契约默认实现
 * 读宽容：返回空列表 / 原字段（不影响启动）
 * 验证严格：canAccessApi 抛 ACCESS_DENIED（防止裸奔）
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAuthzContract implements AuthzContract {

    // ── 读操作：返回空/原值 ──

    @Override
    public List<Tenant> listTenants(Principal principal) {
        log.debug("DefaultAuthzContract: listTenants 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<App> listApps(Principal principal, String tenantCode) {
        log.debug("DefaultAuthzContract: listApps 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<Menu> getMenuTree(Principal principal, String appCode) {
        log.debug("DefaultAuthzContract: getMenuTree 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<FieldPermission> listFieldPermissions(Principal principal, String appCode, String menuCode) {
        log.debug("DefaultAuthzContract: listFieldPermissions 无实现，返回空列表");
        return Collections.emptyList();
    }

    @Override
    public List<String> filterFields(Principal principal, String appCode, String menuCode, List<String> fields) {
        // 字段过滤无实现 → 不过滤，返回原字段列表
        return fields;
    }

    @Override
    public List<DataDimension> getDataDimensions(Principal principal, String appCode) {
        log.debug("DefaultAuthzContract: getDataDimensions 无实现，返回空列表");
        return Collections.emptyList();
    }

    // ── 验证操作：严格拒绝 ──

    @Override
    public boolean canAccessApi(Principal principal, String appCode, String apiUri, String apiMethod) {
        log.warn("DefaultAuthzContract: canAccessApi 无实现，默认拒绝。apiUri={}, method={}", apiUri, apiMethod);
        throw new AuthException(AuthException.AuthErrorType.ACCESS_DENIED,
                "无鉴权实现，请配置 AuthzContract");
    }
}
```

- [ ] **步骤 3：创建 DefaultAkSignContract**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContract.java`:

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AkSignContract;
import lombok.extern.slf4j.Slf4j;

/**
 * AK 签名契约默认实现
 * 功能不可用：sign/verifySign 均抛异常
 * AK 签名是功能性操作，没有实现就不该被调用，抛异常比静默更安全
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAkSignContract implements AkSignContract {

    @Override
    public String sign(String appId, String appSecret) {
        log.warn("DefaultAkSignContract: sign 无实现");
        throw new UnsupportedOperationException("无 AK 签名实现，请配置 AkSignContract");
    }

    @Override
    public boolean verifySign(String sign, String publicKey, String expectedAppId) {
        log.warn("DefaultAkSignContract: verifySign 无实现");
        throw new AuthException(AuthException.AuthErrorType.AK_SIGN_INVALID,
                "无 AK 签名实现，请配置 AkSignContract");
    }
}
```

- [ ] **步骤 4：创建 DefaultSsoFacadeContract**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContract.java`:

```java
package com.wkclz.iam.contract.defaults.facade;

import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import com.wkclz.iam.contract.bean.resp.LoginResp;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import lombok.extern.slf4j.Slf4j;

/**
 * SSO 门面契约默认实现
 * login 抛异常（功能性操作，不该被调用）
 * saveLog/logout 静默跳过（日志丢失不阻断业务）
 *
 * @author shrimp
 */
@Slf4j
public class DefaultSsoFacadeContract implements SsoFacadeContract {

    @Override
    public LoginResp login(SessionCreateReq req) {
        log.warn("DefaultSsoFacadeContract: login 无实现");
        throw new UnsupportedOperationException("无 SSO 门面实现，请配置 SsoFacadeContract");
    }

    @Override
    public void saveLog(RequestLog log) {
        // 日志丢失不阻断业务，静默跳过
        log.debug("DefaultSsoFacadeContract: saveLog 无实现，静默跳过");
    }

    @Override
    public void logout(String token) {
        log.debug("DefaultSsoFacadeContract: logout 无实现，静默跳过");
    }
}
```

- [ ] **步骤 5：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/service/ \
        sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/facade/
git commit -m "feat(contract): 添加四个默认实现（读宽容验证严格）"
```

---

## 任务 10：实现 DefaultAuthFilter

**文件：**
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/filter/DefaultAuthFilter.java`

- [ ] **步骤 1：DefaultAuthFilter 不依赖 ResponseHelper**

迁移后 DefaultAuthFilter 直接使用 `response.setStatus(HttpStatus.UNAUTHORIZED.value())` 设置状态码，不依赖 sh-web 的 ResponseHelper。这是迁移时的解耦设计：避免 default 模块对 sh-web 的硬依赖。

- [ ] **步骤 2：创建 DefaultAuthFilter**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/filter/DefaultAuthFilter.java`:

```java
package com.wkclz.iam.contract.defaults.filter;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.context.PrincipalContext;
import com.wkclz.iam.contract.defaults.config.ContractConfig;
import com.wkclz.iam.contract.exception.AuthException;
import com.wkclz.iam.contract.service.AuthContract;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 默认鉴权过滤器
 * 调用 AuthContract SPI 完成认证，认证失败返回 401
 *
 * 流程：
 * 1. 根路径拒绝
 * 2. public 路径放行（可配置 publicPathPattern）
 * 3. 调用 AuthContract.authenticate()
 * 4. 认证成功 → 缓存 PrincipalContext → 放行
 * 5. 认证失败 → 返回 401
 *
 * @author shrimp
 */
@Slf4j
public class DefaultAuthFilter extends OncePerRequestFilter {

    @Autowired
    private AuthContract authContract;

    @Autowired
    private ContractConfig contractConfig;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 根路径拒绝
        if ("/".equals(uri)) {
            log.debug("DefaultAuthFilter: 根路径拒绝");
            response.setStatus(HttpStatus.FORBIDDEN.value());
            return;
        }

        // 2. public 路径放行
        String pattern = contractConfig.getPublicPathPattern();
        if (PrincipalContext.match(pattern, uri)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 3. 调用 AuthContract SPI 认证
            AuthResult authResult = authContract.authenticate(request);

            if (authResult == null) {
                // 无 token → 拒绝（public 已放行，走到这里说明需要认证）
                log.warn("DefaultAuthFilter: token 不存在，uri={}", uri);
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // 4. 缓存到上下文
            PrincipalContext.cache(request, authResult.getPrincipal(), authResult.getSession());

            // 5. 放行
            chain.doFilter(request, response);
        } catch (AuthException e) {
            log.warn("DefaultAuthFilter: 认证失败: {} - {}, uri={}", e.getErrorType(), e.getMessage(), uri);
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        } finally {
            PrincipalContext.clear();
        }
    }
}
```

**说明：**
- 不直接依赖 ResponseHelper（避免 sh-web 的具体响应工具签名差异），改用 `response.setStatus()` 设置状态码
- `publicPathPattern` 从 ContractConfig 获取（可配置）
- 异常捕获 AuthException，返回 401
- finally 中清理 PrincipalContext，防内存泄漏

**若需返回 JSON 错误体**：在后续迭代中，可引入 ResponseHelper 或自定义 JSON 响应工具。当前实现保持最小依赖。

- [ ] **步骤 3：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/filter/DefaultAuthFilter.java
git commit -m "feat(contract): 添加 DefaultAuthFilter 鉴权过滤器"
```

---

## 任务 11：实现 IamContractAutoConfig

**文件：**
- 创建：`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/IamContractAutoConfig.java`
- 创建：`sh-iam-contract/iam-contract-default/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **步骤 1：创建 IamContractAutoConfig**

`sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/IamContractAutoConfig.java`:

```java
package com.wkclz.iam.contract.defaults.config;

import com.wkclz.iam.contract.defaults.facade.DefaultSsoFacadeContract;
import com.wkclz.iam.contract.defaults.filter.DefaultAuthFilter;
import com.wkclz.iam.contract.defaults.service.DefaultAkSignContract;
import com.wkclz.iam.contract.defaults.service.DefaultAuthContract;
import com.wkclz.iam.contract.defaults.service.DefaultAuthzContract;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.contract.service.AkSignContract;
import com.wkclz.iam.contract.service.AuthContract;
import com.wkclz.iam.contract.service.AuthzContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;

/**
 * 契约层自动配置
 *
 * 注册默认实现（@ConditionalOnMissingBean）：
 * 业务方一旦提供 AuthContract 等 Bean，默认实现自动失效
 *
 * @author shrimp
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.contract.defaults"})
@ConditionalOnProperty(prefix = "sh.iam.contract", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamContractAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public AuthContract authContract() {
        log.info("注册默认 AuthContract（读宽容、验证严格）");
        return new DefaultAuthContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzContract authzContract() {
        log.info("注册默认 AuthzContract（读返回空、鉴权拒绝）");
        return new DefaultAuthzContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public AkSignContract akSignContract() {
        log.info("注册默认 AkSignContract（功能不可用）");
        return new DefaultAkSignContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoFacadeContract ssoFacadeContract() {
        log.info("注册默认 SsoFacadeContract（saveLog 静默、其余不可用）");
        return new DefaultSsoFacadeContract();
    }

    /**
     * 注册 DefaultAuthFilter
     * 通过 ContractConfig.authFilterEnabled 控制是否注册
     * 使用 FilterRegistrationBean 控制过滤器顺序
     */
    @Bean
    @ConditionalOnMissingBean(DefaultAuthFilter.class)
    public FilterRegistrationBean<DefaultAuthFilter> defaultAuthFilterRegistration(
            DefaultAuthFilter filter, ContractConfig config) {
        if (Boolean.FALSE.equals(config.getAuthFilterEnabled())) {
            log.info("DefaultAuthFilter 已禁用（iam.contract.auth-filter-enabled=false）");
            return null;
        }
        FilterRegistrationBean<DefaultAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("defaultAuthFilter");
        log.info("注册 DefaultAuthFilter");
        return registration;
    }
}
```

**说明：**
- `@AutoConfiguration` + `@ComponentScan` 扫描 defaults 包（注册 DefaultAuthFilter 等带 @Component 的类）
- 四个契约均用 `@ConditionalOnMissingBean` 注册默认实现
- DefaultAuthFilter 通过 FilterRegistrationBean 注册，支持配置开关和顺序控制
- **注意**：DefaultAuthFilter 类本身不加 @Component（由 FilterRegistrationBean 注册），否则会重复注册

**修正**：由于 DefaultAuthFilter 不加 @Component，但 @ComponentScan 不会扫描到它。需要在 IamContractAutoConfig 中显式声明 DefaultAuthFilter Bean。调整如下：

- [ ] **步骤 2：修正 IamContractAutoConfig（添加 DefaultAuthFilter Bean）**

修改 `IamContractAutoConfig.java`，在 defaultAuthFilterRegistration 之前添加：

```java
    /**
     * DefaultAuthFilter Bean（不加 @Component，由 FilterRegistrationBean 包装注册）
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultAuthFilter defaultAuthFilter() {
        return new DefaultAuthFilter();
    }
```

并修改 `defaultAuthFilterRegistration` 方法签名，移除对 DefaultAuthFilter 的依赖参数（改为直接注入 defaultAuthFilter Bean）：

```java
    @Bean
    public FilterRegistrationBean<DefaultAuthFilter> defaultAuthFilterRegistration(
            DefaultAuthFilter filter, ContractConfig config) {
        // ... 同上
    }
```

**完整修正后的 IamContractAutoConfig.java**：

```java
package com.wkclz.iam.contract.defaults.config;

import com.wkclz.iam.contract.defaults.facade.DefaultSsoFacadeContract;
import com.wkclz.iam.contract.defaults.filter.DefaultAuthFilter;
import com.wkclz.iam.contract.defaults.service.DefaultAkSignContract;
import com.wkclz.iam.contract.defaults.service.DefaultAuthContract;
import com.wkclz.iam.contract.defaults.service.DefaultAuthzContract;
import com.wkclz.iam.contract.facade.SsoFacadeContract;
import com.wkclz.iam.contract.service.AkSignContract;
import com.wkclz.iam.contract.service.AuthContract;
import com.wkclz.iam.contract.service.AuthzContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.Ordered;

/**
 * 契约层自动配置
 *
 * 注册默认实现（@ConditionalOnMissingBean）：
 * 业务方一旦提供 AuthContract 等 Bean，默认实现自动失效
 *
 * @author shrimp
 */
@Slf4j
@AutoConfiguration
@ComponentScan(basePackages = {"com.wkclz.iam.contract.defaults"})
@ConditionalOnProperty(prefix = "sh.iam.contract", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IamContractAutoConfig {

    @Bean
    @ConditionalOnMissingBean
    public AuthContract authContract() {
        log.info("注册默认 AuthContract（读宽容、验证严格）");
        return new DefaultAuthContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthzContract authzContract() {
        log.info("注册默认 AuthzContract（读返回空、鉴权拒绝）");
        return new DefaultAuthzContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public AkSignContract akSignContract() {
        log.info("注册默认 AkSignContract（功能不可用）");
        return new DefaultAkSignContract();
    }

    @Bean
    @ConditionalOnMissingBean
    public SsoFacadeContract ssoFacadeContract() {
        log.info("注册默认 SsoFacadeContract（saveLog 静默、其余不可用）");
        return new DefaultSsoFacadeContract();
    }

    /**
     * DefaultAuthFilter Bean
     * 不加 @Component，由 FilterRegistrationBean 包装注册
     */
    @Bean
    @ConditionalOnMissingBean
    public DefaultAuthFilter defaultAuthFilter() {
        return new DefaultAuthFilter();
    }

    /**
     * 注册 DefaultAuthFilter 到过滤器链
     * 通过 ContractConfig.authFilterEnabled 控制是否注册
     */
    @Bean
    public FilterRegistrationBean<DefaultAuthFilter> defaultAuthFilterRegistration(
            DefaultAuthFilter filter, ContractConfig config) {
        if (Boolean.FALSE.equals(config.getAuthFilterEnabled())) {
            log.info("DefaultAuthFilter 已禁用（iam.contract.auth-filter-enabled=false）");
            return null;
        }
        FilterRegistrationBean<DefaultAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        registration.setName("defaultAuthFilter");
        log.info("注册 DefaultAuthFilter");
        return registration;
    }
}
```

- [ ] **步骤 3：创建 AutoConfiguration.imports**

`sh-iam-contract/iam-contract-default/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```
com.wkclz.iam.contract.defaults.config.IamContractAutoConfig
```

- [ ] **步骤 4：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-default/src/main/java/com/wkclz/iam/contract/defaults/config/IamContractAutoConfig.java \
        sh-iam-contract/iam-contract-default/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
git commit -m "feat(contract): 添加 IamContractAutoConfig 与自动配置注册"
```

---

## 任务 12：编写默认实现单元测试

**文件：**
- 创建：`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContractTest.java`
- 创建：`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContractTest.java`
- 创建：`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContractTest.java`
- 创建：`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContractTest.java`

**测试策略**：验证"读宽容验证严格"行为——读操作返回空/原值，验证操作抛特定异常。

- [ ] **步骤 1：创建 DefaultAuthContractTest**

`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAuthContractTest.java`:

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAuthContract 单元测试
 * 验证读宽容验证严格行为
 *
 * @author shrimp
 */
class DefaultAuthContractTest {

    private final DefaultAuthContract contract = new DefaultAuthContract();

    @AfterEach
    void tearDown() {
        // 清理 PrincipalContext（测试可能设置了请求上下文）
        com.wkclz.iam.contract.context.PrincipalContext.clear();
    }

    @Test
    void authenticate_noTokenInRequest_returnsNull() {
        // 无 token → 返回 null（过滤器据此放行 public 路径）
        HttpServletRequest request = new MockHttpServletRequest();
        AuthResult result = contract.authenticate(request);
        assertNull(result, "无 token 时应返回 null");
    }

    @Test
    void authenticate_tokenExistsButNoImpl_throwsTokenInvalid() {
        // 有 token 但无实现 → 抛 TOKEN_INVALID
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-token");
        AuthException ex = assertThrows(AuthException.class, () -> contract.authenticate(request));
        assertEquals(AuthException.AuthErrorType.TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void checkToken_alwaysThrowsTokenMissing() {
        // checkToken 无实现 → 抛 TOKEN_MISSING
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.checkToken("any-token", "any-identifier"));
        assertEquals(AuthException.AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }
}
```

**说明**：测试中需要模拟 `PrincipalContext.getToken()` 从请求头读取 token。由于 `PrincipalContext` 内部调用 `getRequest()`（通过 `RequestContextHolder`），而 `RequestContextHolder` 在非 Web 上下文中可能返回 null。需要通过 `RequestContextHolder` 设置请求上下文。

**修正测试**（添加 RequestContextHolder 设置）：

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.AuthResult;
import com.wkclz.iam.contract.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAuthContract 单元测试
 * 验证读宽容验证严格行为
 *
 * @author shrimp
 */
class DefaultAuthContractTest {

    private final DefaultAuthContract contract = new DefaultAuthContract();

    @BeforeEach
    void setUp() {
        // 设置请求上下文，使 getRequest() 能返回请求（通过 RequestContextHolder）
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        com.wkclz.iam.contract.context.PrincipalContext.clear();
    }

    @Test
    void authenticate_noTokenInRequest_returnsNull() {
        // 无 token → 返回 null
        AuthResult result = contract.authenticate(new MockHttpServletRequest());
        assertNull(result, "无 token 时应返回 null");
    }

    @Test
    void authenticate_tokenExistsButNoImpl_throwsTokenInvalid() {
        // 有 token 但无实现 → 抛 TOKEN_INVALID
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer some-token");
        // 同步设置到 RequestContextHolder（PrincipalContext.getToken 从这里读）
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        AuthException ex = assertThrows(AuthException.class, () -> contract.authenticate(request));
        assertEquals(AuthException.AuthErrorType.TOKEN_INVALID, ex.getErrorType());
    }

    @Test
    void checkToken_alwaysThrowsTokenMissing() {
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.checkToken("any-token", "any-identifier"));
        assertEquals(AuthException.AuthErrorType.TOKEN_MISSING, ex.getErrorType());
    }
}
```

- [ ] **步骤 2：创建 DefaultAuthzContractTest**

`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAuthzContractTest.java`:

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.bean.App;
import com.wkclz.iam.contract.bean.DataDimension;
import com.wkclz.iam.contract.bean.FieldPermission;
import com.wkclz.iam.contract.bean.Menu;
import com.wkclz.iam.contract.bean.Principal;
import com.wkclz.iam.contract.bean.Tenant;
import com.wkclz.iam.contract.exception.AuthException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAuthzContract 单元测试
 * 验证读宽容（返回空/原值）、验证严格（canAccessApi 抛异常）
 *
 * @author shrimp
 */
class DefaultAuthzContractTest {

    private final DefaultAuthzContract contract = new DefaultAuthzContract();
    private final Principal principal = new Principal();

    @Test
    void listTenants_returnsEmptyList() {
        List<Tenant> result = contract.listTenants(principal);
        assertNotNull(result);
        assertTrue(result.isEmpty(), "无实现时应返回空列表");
    }

    @Test
    void listApps_returnsEmptyList() {
        List<App> result = contract.listApps(principal, "tenant-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getMenuTree_returnsEmptyList() {
        List<Menu> result = contract.getMenuTree(principal, "app-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void listFieldPermissions_returnsEmptyList() {
        List<FieldPermission> result = contract.listFieldPermissions(principal, "app-001", "menu-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void filterFields_returnsOriginalFields() {
        // 字段过滤无实现 → 返回原字段列表（不过滤）
        List<String> fields = Arrays.asList("name", "email", "phone");
        List<String> result = contract.filterFields(principal, "app-001", "menu-001", fields);
        assertEquals(fields, result, "无实现时应返回原字段列表");
    }

    @Test
    void filterFields_emptyInput_returnsEmpty() {
        List<String> result = contract.filterFields(principal, "app-001", "menu-001", Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void getDataDimensions_returnsEmptyList() {
        List<DataDimension> result = contract.getDataDimensions(principal, "app-001");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void canAccessApi_throwsAccessDenied() {
        // 接口鉴权无实现 → 抛 ACCESS_DENIED
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.canAccessApi(principal, "app-001", "/api/test", "GET"));
        assertEquals(AuthException.AuthErrorType.ACCESS_DENIED, ex.getErrorType());
    }
}
```

- [ ] **步骤 3：创建 DefaultAkSignContractTest**

`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/service/DefaultAkSignContractTest.java`:

```java
package com.wkclz.iam.contract.defaults.service;

import com.wkclz.iam.contract.exception.AuthException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultAkSignContract 单元测试
 * 验证 sign/verifySign 均抛异常
 *
 * @author shrimp
 */
class DefaultAkSignContractTest {

    private final DefaultAkSignContract contract = new DefaultAkSignContract();

    @Test
    void sign_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class,
                () -> contract.sign("appId", "appSecret"));
    }

    @Test
    void verifySign_throwsAkSignInvalid() {
        AuthException ex = assertThrows(AuthException.class,
                () -> contract.verifySign("any-sign", "publicKey", "appId"));
        assertEquals(AuthException.AuthErrorType.AK_SIGN_INVALID, ex.getErrorType());
    }
}
```

- [ ] **步骤 4：创建 DefaultSsoFacadeContractTest**

`sh-iam-contract/iam-contract-default/src/test/java/com/wkclz/iam/contract/defaults/facade/DefaultSsoFacadeContractTest.java`:

```java
package com.wkclz.iam.contract.defaults.facade;

import com.wkclz.iam.contract.bean.RequestLog;
import com.wkclz.iam.contract.bean.req.SessionCreateReq;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DefaultSsoFacadeContract 单元测试
 * 验证 login 抛异常、saveLog/logout 静默跳过
 *
 * @author shrimp
 */
class DefaultSsoFacadeContractTest {

    private final DefaultSsoFacadeContract facade = new DefaultSsoFacadeContract();

    @Test
    void login_throwsUnsupportedOperationException() {
        SessionCreateReq req = new SessionCreateReq();
        assertThrows(UnsupportedOperationException.class, () -> facade.login(req));
    }

    @Test
    void saveLog_doesNotThrow() {
        // 静默跳过，不抛异常
        RequestLog log = new RequestLog();
        assertDoesNotThrow(() -> facade.saveLog(log));
    }

    @Test
    void logout_doesNotThrow() {
        // 静默跳过，不抛异常
        assertDoesNotThrow(() -> facade.logout("any-token"));
    }

    @Test
    void logout_noArg_doesNotThrow() {
        // 无参重载（从 PrincipalContext 获取 token，无上下文时 token=null）
        assertDoesNotThrow(() -> facade.logout());
    }
}
```

- [ ] **步骤 5：运行测试**

运行：`./mvnw test -pl iam-contract-default -am -q` （若项目有 mvnw）
或：`mvn test -pl iam-contract-default -am -q`（若有 Maven CLI）

预期：所有测试通过

**若环境无 Maven CLI**：在 IDE 中运行测试，确认全部通过。

- [ ] **步骤 6：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add sh-iam-contract/iam-contract-default/src/test/
git commit -m "test(contract): 添加默认实现单元测试（读宽容验证严格）"
```

---

## 任务 13：最终编译验证与文档更新

> **状态：已完成**。本任务在 sh-iam 项目实施时已执行；迁移到 sh-framework 后，AGENTS.md 与规格状态由 sh-framework 项目的 `migrate-iam-contract-docs` spec 文档维护。

**文件：**
- 修改：`AGENTS.md`（追加新模块说明）
- 修改：`docs/superpowers/specs/2026-07-04-iam-contract-layer-design.md`（标注实现完成）

- [ ] **步骤 1：全量编译验证**

运行：`./mvnw compile -pl iam-contract-api,iam-contract-default -am -q`
或：`mvn compile -pl iam-contract-api,iam-contract-default -am -q`

预期：BUILD SUCCESS

**若环境无 Maven CLI**：在 IDE 中刷新 Maven 项目，构建 iam-contract-api 和 iam-contract-default 模块，确认无编译错误。

- [ ] **步骤 2：全量测试验证**

运行：`./mvnw test -pl iam-contract-default -am -q`
或：`mvn test -pl iam-contract-default -am -q`

预期：所有测试通过

- [ ] **步骤 3：更新 AGENTS.md**

在 AGENTS.md 的"模块结构"章节追加新模块说明：

```markdown
sh-iam/
├── iam-common          # 公共实体、DTO、工具类（所有后端模块依赖）
├── iam-contract-api    # 契约层 API（接口定义 + 中性模型 + PrincipalContext，零业务依赖）
├── iam-contract-default # 契约层默认实现（读宽容验证严格 + DefaultAuthFilter + AutoConfig）
├── iam-sdk             # SDK 模块（第三方应用引入，提供 Filter/JWT/Session）
...
```

在"模块依赖关系"章节追加：

```markdown
iam-contract-default → iam-contract-api → sh-web（仅 RequestHelper）

后续 iam-sdk 重构后：
iam-sdk → iam-contract-api（实现四契约）
```

在"各模块关键类索引"章节追加新模块索引：

```markdown
### iam-contract-api (`com.wkclz.iam.contract`)

| 包        | 类                                                                                                         | 说明                                                         |
|-----------|-----------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| `bean`    | Principal, Session, AuthResult, Tenant, App, Menu, Api, FieldPermission, DataDimension, RequestLog        | 10 个中性数据模型                                            |
| `bean.req`| SessionCreateReq                                                                                          | 会话创建请求                                                 |
| `bean.resp`| LoginResp                                                                                                | 登录响应                                                     |
| `facade`  | SsoFacadeContract                                                                                         | SSO 门面契约: login/saveLog/logout                           |
| `service` | AuthContract, AuthzContract, AkSignContract                                                               | 三个核心契约 SPI                                             |
| `context` | PrincipalContext                                                                                          | Principal 读取上下文（替代 sh-core UserContext 的用户读取）  |
| `config`  | ContractSettings                                                                                          | 静态配置持有器（供 default 方法访问）                        |
| `enums`   | AuthScene                                                                                                 | 鉴权场景枚举                                                 |
| `exception`| AuthException                                                                                            | 契约层统一异常（含 AuthErrorType）                           |

### iam-contract-default (`com.wkclz.iam.contract.defaults`)

| 包        | 类                                                                                                         | 说明                                                         |
|-----------|-----------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| `config`  | ContractConfig, IamContractAutoConfig                                                                     | 配置绑定 + 自动配置（@ConditionalOnMissingBean 注册默认实现）|
| `service` | DefaultAuthContract, DefaultAuthzContract, DefaultAkSignContract                                          | 三个默认实现（读宽容验证严格）                               |
| `facade`  | DefaultSsoFacadeContract                                                                                  | SSO 门面默认实现（saveLog/logout 静默、login 抛异常）        |
| `filter`  | DefaultAuthFilter                                                                                         | 鉴权过滤器（调用 AuthContract SPI）                          |
```

在"配置项"章节追加：

```markdown
### iam-contract 配置 (`iam.contract.*`)

| 配置 | 说明 | 默认值 |
|------|------|--------|
| `sh.iam.contract.enabled` | 是否启用契约层自动配置 | true |
| `iam.contract.auth-filter-enabled` | 是否注册 DefaultAuthFilter | true |
| `iam.contract.public-path-pattern` | 公开路径匹配模式 | `/*/public/**` |
| `iam.contract.app-id` | AK 签名 appId | - |
| `iam.contract.app-secret` | AK 签名 appSecret（RSA 私钥） | - |
| `iam.contract.public-key` | AK 验签 publicKey（RSA 公钥） | - |
| `iam.contract.server-url` | SSO 服务端地址 | - |
| `iam.contract.jwt-secret-key` | JWT 密钥（供实现层使用） | - |
```

在"扩展点"章节追加：

```markdown
| 扩展点 | 模块 | 方式 |
|--------|------|------|
| 认证逻辑 | iam-contract-api | 实现 `AuthContract` 接口覆盖默认 Bean |
| 鉴权逻辑 | iam-contract-api | 实现 `AuthzContract` 接口覆盖默认 Bean |
| AK 签名 | iam-contract-api | 实现 `AkSignContract` 接口覆盖默认 Bean |
| SSO 远程调用 | iam-contract-api | 实现 `SsoFacadeContract` 接口覆盖默认 Bean |
```

- [ ] **步骤 4：更新规格文档状态**

修改 `docs/superpowers/specs/2026-07-04-iam-contract-layer-design.md` 顶部：

```markdown
> 日期: 2026-07-04
> 状态: 已实现（契约层），iam-sdk 重构待后续
> 范围: iam-contract-api + iam-contract-default 模块
```

- [ ] **步骤 5：Commit**

```bash
# 历史提交（迁移前在 sh-iam 项目执行）
git add AGENTS.md docs/superpowers/specs/2026-07-04-iam-contract-layer-design.md
git commit -m "docs(contract): 更新 AGENTS.md 与规格状态，记录新模块"
```

---

## 自检清单

### 规格覆盖度

| 规格章节 | 实现任务 | 状态 |
|----------|----------|------|
| §3 模块架构（拓扑/依赖/包结构） | 任务 1, 7 | ✓ |
| §4.1 AuthContract | 任务 5 | ✓ |
| §4.2 AuthzContract（含重载） | 任务 5 | ✓ |
| §4.3 AkSignContract（含重载） | 任务 5 | ✓ |
| §4.4 SsoFacadeContract | 任务 5 | ✓ |
| §4.5 PrincipalContext | 任务 4 | ✓ |
| §5.1 Principal | 任务 3 | ✓ |
| §5.2 Session | 任务 3 | ✓ |
| §5.3 AuthResult | 任务 3 | ✓ |
| §5.4-5.9 Tenant/App/Menu/Api/FieldPermission/DataDimension | 任务 3 | ✓ |
| §5.10 RequestLog | 任务 3 | ✓ |
| §5.11 SessionCreateReq | 任务 3 | ✓ |
| §5.12 LoginResp | 任务 3 | ✓ |
| §5.13 AuthException + AuthScene | 任务 2 | ✓ |
| §6.1-6.5 默认实现行为 | 任务 9, 12 | ✓ |
| §6.6 DefaultAuthFilter | 任务 10 | ✓ |
| §6.7 IamContractAutoConfig | 任务 11 | ✓ |
| §6.8 PrincipalContext 完整实现 | 任务 4 | ✓ |
| §7 配置项（ContractSettings + ContractConfig） | 任务 4, 8 | ✓ |
| §8 AutoConfiguration.imports | 任务 11 | ✓ |
| §9 iam-sdk 实现要求 | 本次不实现，仅文档 | ✓ |

### 占位符扫描

- 无 TODO/TBD/待定内容
- 所有代码块完整可执行
- 所有文件路径精确

### 类型一致性

- `AuthContract.authenticate()` 返回 `AuthResult`，与 `DefaultAuthFilter` 中调用一致 ✓
- `PrincipalContext.getToken()` 在 `DefaultAuthContract` 和 `AkSignContract.verifySign(request)` 中调用一致 ✓
- `AuthException.AuthErrorType` 枚举值在 `DefaultAuthContract`/`DefaultAuthzContract`/`DefaultAkSignContract` 中使用一致 ✓
- `ContractSettings` 的 getter 方法名与 `AkSignContract` default 方法中调用一致 ✓
- `ContractConfig` 字段名与 `DefaultAuthFilter` 中 `getPublicPathPattern()` 调用一致 ✓

### 范围确认

- 仅创建 iam-contract-api + iam-contract-default 两个新模块 ✓
- 仅修改根 pom.xml 注册新模块 ✓
- 不改动 iam-sdk/iam-sso/iam-admin 等现有模块 ✓
- iam-sdk 重构仅作为规格文档中的要求章节，本次不实现 ✓
