# 字符串格式化与 Bean 操作工具
- **所属模块**：sh-tool
- **优先级**：高
- **故事ID**：US-028

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 使用 StringFormat 的 {} 占位符和 ${var} 命名变量进行字符串格式化，以及 BeanUtil 进行属性拷贝，
**以便于** 统一异常消息格式化方式，减少 Bean 转换的样板代码。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 调用 StringFormat.of("用户 {} 不存在", "admin"), When 格式化, Then 返回 "用户 admin 不存在"
- [场景2] Given 模板 "Dear ${name}, your order ${orderId}[(${amount}元)] is ready", When params 中 name=John, orderId=001, amount 为空, Then 输出 "Dear John, your order 001 is ready"（条件渲染语法，amount为空时括号内内容不渲染）
- [场景3] Given 调用 BeanUtil.cpNotNull(source, target), When source 的某字段为 null, Then target 对应字段保持原值不被覆盖
- [异常场景] Given StringFormat.of() 参数不足, When 格式化, Then 多余的 {} 保留原样不替换

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-tool/src/main/java/com/wkclz/tool/utils/StringFormat.java` (字符串格式化，{}占位符+${var}命名变量+条件渲染)
- `sh-tool/src/main/java/com/wkclz/tool/utils/BeanUtil.java` (Bean操作，cpAll/cpNotNull/removeBlank/getJavaField)
- `sh-tool/src/main/java/com/wkclz/tool/utils/StringUtil.java` (字符串工具，下划线/驼峰转换)
- `sh-tool/src/main/java/com/wkclz/tool/bean/JavaField.java` (字段元数据，fieldName/columnName/getter/setter)
