# 实体体系与数据规范
- **所属模块**：sh-core
- **优先级**：高
- **故事ID**：US-001

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 所有业务实体继承统一的基类（DbColumnEntity → BaseEntity），
**以便于** 自动获得数据库规范字段、分页参数、查询辅助字段和属性拷贝能力，减少重复编码。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 一个新的业务实体类, When 它继承 BaseEntity, Then 自动拥有 id/sort/createTime/createBy/updateTime/updateBy/remark/version 共 8 个数据库规范字段
- [场景2] Given 一个 BaseEntity 实例, When 调用 init() 方法, Then current 默认为 1、size 默认为 10、offset 自动计算为 (current-1)*size
- [场景3] Given 两个 BaseEntity 子类实例 source 和 target, When 调用 BaseEntity.copy(source, target), Then source 的所有属性值被拷贝到 target
- [异常场景] Given source 为 null, When 调用 BaseEntity.copy(source, target), Then 返回 null 而不抛出异常

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-core/src/main/java/com/wkclz/core/base/DbColumnEntity.java` (数据库规范字段基类，定义8个标准字段)
- `sh-core/src/main/java/com/wkclz/core/base/BaseEntity.java` (业务实体基类，扩展分页/查询/用户租户字段)
- `sh-core/src/main/java/com/wkclz/core/base/Pageable.java` (分页接口，定义分页参数的获取与初始化)
- `sh-core/src/main/java/com/wkclz/core/annotation/FieldDesc.java` (字段描述注解，标注字段含义和约束)
- `sh-tool/src/main/java/com/wkclz/tool/utils/BeanUtil.java` (Bean属性拷贝工具，支撑copy/copyIfNotNull)
