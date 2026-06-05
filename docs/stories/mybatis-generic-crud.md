# 通用 Mapper 与动态 SQL 生成
- **所属模块**：sh-mybatis
- **优先级**：高
- **故事ID**：US-007

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 定义实体类后，继承 BaseMapper 即可自动获得 14 个通用 CRUD 方法，
**以便于** 无需手写任何 SQL 即可完成单表的增删改查操作，大幅提升开发效率。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 定义 UserMapper extends BaseMapper<User>, When 注入 UserMapper, Then 自动拥有 insert/insertBatch/deleteById/deleteByIds/updateById/updateByIdSelective/updateBatch/selectById/selectByIds/selectAll/selectByEntity/selectByEntityWithLimit/selectCountByEntity/selectOneByEntity 共 14 个方法
- [场景2] Given 调用 insert(entity), When 实体 id 为 null, Then 插入后 id 自动回填（useGeneratedKeys=true）
- [场景3] Given 调用 insertBatch(entities), When 列表超过 1000 条, Then BaseService 自动分批插入，每批最多 1000 条
- [异常场景] Given 实体中 @FieldDesc(notNull=true) 的字段为 null, When 调用 insert, Then 抛出 ValidationException

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/BaseMapper.java` (通用Mapper接口，14个CRUD方法声明)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/BaseMapperProvider.java` (SQL Provider基类，缓存/WHERE/ORDER BY构建)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/InsertMapperProvider.java` (插入SQL生成，notNull校验)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/mapper/impl/InsertBatchMapperProvider.java` (批量插入SQL生成)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/bean/DbEntityProperty.java` (实体元数据，字段分组与缓存)
