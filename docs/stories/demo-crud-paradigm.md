# 示例模块 CRUD 标准范式
- **所属模块**：sh-demo
- **优先级**：高
- **故事ID**：US-030

## 1. 用户故事 (User Story)
**作为** 新加入团队的开发者，
**我希望** 参考示例模块的完整 CRUD 实现（Entity → Mapper → Service → VO → Route → Rest），
**以便于** 快速掌握框架的使用方式，按照标准范式开发新的业务模块。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 定义 User extends BaseEntity + UserMapper extends BaseMapper<User>, When 无需手写 SQL, Then 自动拥有 14 个 CRUD 方法
- [场景2] Given 定义 UserService extends BaseService<User, UserMapper>, When 调用 userService.selectPage(entity), Then 返回 PageData<User> 分页数据
- [场景3] Given 定义 Route 接口集中管理路径 + UserRest 控制器, When 请求 GET /sh-demo/user/page, Then 返回 R<PageData<UserPageResp>>
- [异常场景] Given 查询的用户 ID 不存在, When 调用 userService.selectById(id) 结果为 null, Then 抛出 NotFoundException

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-demo/src/main/java/com/wkclz/demo/bean/entity/User.java` (用户实体，extends BaseEntity)
- `sh-demo/src/main/java/com/wkclz/demo/mapper/UserMapper.java` (用户Mapper，extends BaseMapper<User>)
- `sh-demo/src/main/java/com/wkclz/demo/service/UserService.java` (用户Service，extends BaseService)
- `sh-demo/src/main/java/com/wkclz/demo/rest/UserRest.java` (用户REST控制器，CRUD接口)
- `sh-demo/src/main/java/com/wkclz/demo/rest/Route.java` (路由常量接口，@Router+@ApiDesc)
- `sh-demo/src/main/java/com/wkclz/demo/bean/vo/user/UserCreateReq.java` (创建请求VO，@NotBlank校验)
- `sh-demo/src/main/java/com/wkclz/demo/bean/vo/user/UserUpdateReq.java` (更新请求VO，extends UpdateReq)
- `sh-demo/src/main/java/com/wkclz/demo/bean/vo/user/UserPageReq.java` (分页请求VO，extends PageReq)
- `sh-demo/src/main/java/com/wkclz/demo/bean/vo/user/UserResp.java` (详情响应VO，extends EntityResp)
- `sh-demo/src/main/resources/config/application.yml` (示例配置，数据源+MyBatis+PageHelper)
