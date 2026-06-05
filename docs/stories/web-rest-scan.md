# REST 接口元数据扫描
- **所属模块**：sh-web
- **优先级**：中
- **故事ID**：US-014

## 1. 用户故事 (User Story)
**作为** 系统管理员，
**我希望** 框架自动扫描所有 REST 接口并生成元数据（URI、方法、描述、模块），
**以便于** 实现接口权限自动注册、API 文档生成和接口审计。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 定义了 @RestController 类和 @GetMapping 方法, When 调用 RestHelper.getMapping(), Then 返回包含 URI、HTTP 方法、接口名称的 RestInfo 列表
- [场景2] Given 定义了 @Router(module="用户管理", prefix="/user") 的类, When 扫描接口, Then 同包下的接口自动补充 module 和 desc 信息
- [场景3] Given URI 包含 /public/ 路径, When 扫描接口, Then writeFlag 标记为 1（公开接口）
- [异常场景] Given 接口未添加 @ApiDesc 或 @Desc 注解, When 扫描接口, Then desc 字段为空但不影响扫描

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-web/src/main/java/com/wkclz/web/helper/RestHelper.java` (REST接口扫描工具，提取元数据)
- `sh-web/src/main/java/com/wkclz/web/bean/RestInfo.java` (接口元数据Bean，包含URI/方法/描述/模块)
- `sh-core/src/main/java/com/wkclz/core/annotation/Router.java` (路由注解，声明模块和前缀)
- `sh-core/src/main/java/com/wkclz/core/annotation/ApiDesc.java` (API描述注解)
