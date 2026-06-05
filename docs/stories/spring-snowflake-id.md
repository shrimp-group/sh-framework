# 雪花 ID 与系统初始化
- **所属模块**：sh-spring
- **优先级**：中
- **故事ID**：US-023

## 1. 用户故事 (User Story)
**作为** 业务开发者，
**我希望** 通过 SnowflakeHelper 生成全局唯一的雪花 ID，且系统启动时自动识别运行环境，
**以便于** 分布式环境下生成有序唯一 ID，同时根据环境（DEV/SIT/UAT/PROD）差异化处理。

## 2. 验收标准 (Acceptance Criteria)
- [场景1] Given 调用 SnowflakeHelper.getSnowflakeId(), When 生成 ID, Then 返回 64 位 Long 型唯一 ID
- [场景2] Given spring.profiles.active=prod, When 系统启动, Then Sys.getCurrentEnv() 返回 EnvType.PROD
- [场景3] Given 不同机器的网络接口信息不同, When 计算 workId, Then 不同机器获得不同的 workId（0~31）
- [异常场景] Given 时钟回拨, When SnowflakeIdWorker.nextId() 检测到回拨, Then 抛出 RuntimeException

## 3. 涉及代码与上下文 (AI开发关键)
为了完成或修改此故事，AI 需要重点阅读以下核心代码文件：
- `sh-spring/src/main/java/com/wkclz/spring/helper/SnowflakeHelper.java` (雪花ID辅助类，workId基于网卡/datacenterId基于环境)
- `sh-tool/src/main/java/com/wkclz/tool/utils/SnowflakeIdWorker.java` (雪花算法核心实现，synchronized+时钟回拨检测)
- `sh-spring/src/main/java/com/wkclz/spring/config/Sys.java` (系统初始化器，ApplicationRunner+环境判断)
- `sh-core/src/main/java/com/wkclz/core/enums/EnvType.java` (环境类型枚举，DEV/SIT/UAT/PROD)
