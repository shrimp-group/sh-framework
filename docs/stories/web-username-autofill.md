# 响应体用户名自动填充
- **所属模块**：sh-web
- **优先级**：中
- **故事ID**：US-013

## 1. 用户故事 (User Story)
**作为** 前端开发者，
**我希望** API 响应中的 createBy/updateBy 字段自动转换为 createByName/updateByName，
**以便于** 前端直接展示操作人姓名，无需额外调用用户查询接口。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 响应体中包含 BaseEntity 子类且 createBy="U001", When UserNameBodyAdvice 拦截, Then 通过 UserNameProvider 批量查询后自动填充 createByName
- [场景2] Given 响应体是 R<PageData<UserResp>>, When 递归收集 BaseEntity, Then 能穿透 R/PageData/List 找到所有 BaseEntity 实例
- [场景3] Given 业务系统实现了 UserNameProvider, When DCL 懒加载查找, Then 仅查找一次并缓存
- [异常场景] Given 响应体嵌套深度超过 8 层, When 递归收集, Then 停止递归避免栈溢出

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-web/src/main/java/com/wkclz/web/rest/UserNameBodyAdvice.java` (响应体增强，递归收集BaseEntity并填充用户名)
- `sh-core/src/main/java/com/wkclz/core/spi/UserNameProvider.java` (SPI接口，业务方实现用户名查询)
- `sh-core/src/main/java/com/wkclz/core/base/BaseEntity.java` (createByName/updateByName字段定义)
- `sh-web/src/main/java/com/wkclz/web/bean/EntityResp.java` (标准响应VO，包含createByName/updateByName)
