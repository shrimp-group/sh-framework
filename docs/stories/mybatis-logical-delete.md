# 逻辑删除与数据安全
- **所属模块**：sh-mybatis
- **优先级**：高
- **故事ID**：US-008

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 所有删除操作均为逻辑删除（更新 deleted 字段而非物理删除），
**以便于** 误删数据可恢复，同时所有查询自动过滤已删除数据，保证数据安全性。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 调用 deleteById(id), When 执行 SQL, Then 生成 UPDATE SET deleted = DATE_FORMAT(NOW(6), '%Y%m%d%H%i%s%m') 而非 DELETE
- [场景2] Given 调用 selectByEntity(entity), When 生成 WHERE 条件, Then 自动追加 deleted = 0
- [场景3] Given 调用 deleteByIds(ids), When 传入多个 ID, Then 生成 WHERE id IN (...) AND deleted = 0
- [异常场景] Given deleted 字段值使用微秒级时间戳, When 并发删除同一记录, Then 每条记录的 deleted 值唯一，可区分不同删除操作

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/DeleteByIdMapperProvider.java` (单条逻辑删除SQL生成)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/DeleteByIdsMapperProvider.java` (批量逻辑删除SQL生成)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/BaseMapperProvider.java` (buildWhereClause自动追加deleted=0)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/bean/DbEntityProperty.java` (DELETED_FIELD常量定义)
