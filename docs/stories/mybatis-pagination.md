# 分页查询与 PageData 封装
- **所属模块**：sh-mybatis
- **优先级**：高
- **故事ID**：US-011

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 通过 BaseService.selectPage() 或 PageQuery.page() 实现自动分页查询，
**以便于** 无需手动计算 offset 和拼接 LIMIT，统一获得 PageData 分页数据封装。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given entity.current=2, entity.size=10, When 调用 BaseService.selectPage(), Then 先查 count 获取总数，再查 LIMIT 10,10 获取数据，封装为 PageData
- [场景2] Given entity.current=null, entity.size=null, When 调用 entity.init(), Then current 默认为 1，size 默认为 10
- [场景3] Given 使用 PageQuery.page(param, function), When 执行查询, Then 自动管理 PageHelper.startPage/clearPage 生命周期
- [异常场景] Given 查询结果为空, When 调用 selectPage(), Then 返回 PageData.empty()，records 为空列表，total 为 0

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-mybatis/src/main/java/com/wkclz/mybatis/service/BaseService.java` (通用Service，selectPage手动分页实现)
- `sh-mybatis/src/main/java/com/wkclz/mybatis/helper/PageQuery.java` (PageHelper分页工具，自动管理生命周期)
- `sh-core/src/main/java/com/wkclz/core/base/PageData.java` (分页数据封装类，工厂方法创建)
- `sh-core/src/main/java/com/wkclz/core/base/BaseEntity.java` (init()方法初始化分页参数)
