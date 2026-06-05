# 异常体系与分类处理
- **所属模块**：sh-core
- **优先级**：高
- **故事ID**：US-003

## 1. 用户故事 (User Story)
**作为** 后端开发者，
**我希望** 使用分类明确的异常体系（CommonException 及 7 个子类），
**以便于** 全局异常处理器能按异常类型差异化处理（返回不同 HTTP 状态码、触发邮件告警等）。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 参数校验失败, When 抛出 ValidationException, Then 全局异常处理器返回 HTTP 400 和自定义 code
- [场景2] Given 资源未找到, When 抛出 NotFoundException, Then 全局异常处理器返回 HTTP 404
- [场景3] Given 系统级异常, When 抛出 SystemException.of("message: {}", arg), Then 异常消息通过模板格式化
- [异常场景] Given 异常被多层包装, When 全局异常处理器遍历 3 层 cause 链, Then 能穿透找到被包装的 CommonException

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-core/src/main/java/com/wkclz/core/exception/CommonException.java` (业务异常基类，支持静态工厂方法)
- `sh-core/src/main/java/com/wkclz/core/exception/SystemException.java` (系统级异常子类)
- `sh-core/src/main/java/com/wkclz/core/exception/ValidationException.java` (校验异常子类)
- `sh-core/src/main/java/com/wkclz/core/exception/NotFoundException.java` (资源未找到异常子类)
- `sh-web/src/main/java/com/wkclz/web/rest/ErrorHandler.java` (全局异常处理器，按类型差异化处理)
