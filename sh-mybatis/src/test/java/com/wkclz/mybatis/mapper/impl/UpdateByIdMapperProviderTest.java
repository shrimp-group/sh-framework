package com.wkclz.mybatis.mapper.impl;

import com.wkclz.core.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * UpdateByIdMapperProvider 单元测试
 *
 * @author shrimp
 */
@DisplayName("UpdateByIdMapperProvider 更新 SQL 生成测试")
class UpdateByIdMapperProviderTest {

    @Test
    @DisplayName("entity 为 null 时抛出 ValidationException")
    void testEntityNull() {
        ValidationException exception = assertThrows(ValidationException.class,
            () -> new UpdateByIdMapperProvider().updateById(null),
            "entity 为 null 时应抛出 ValidationException");
        assertEquals("实体对象不能为空", exception.getMessage(), "异常消息应为 '实体对象不能为空'");
    }

    @Test
    @DisplayName("id 为 null 时抛出 ValidationException")
    void testIdNull() {
        TestEntity entity = new TestEntity();
        entity.setVersion(1);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> new UpdateByIdMapperProvider().updateById(entity),
            "id 为 null 时应抛出 ValidationException");
        assertEquals("ID不能为空", exception.getMessage(), "异常消息应为 'ID不能为空'");
    }

    @Test
    @DisplayName("version 为 null 时抛出 ValidationException")
    void testVersionNull() {
        TestEntity entity = new TestEntity();
        entity.setId(1L);

        ValidationException exception = assertThrows(ValidationException.class,
            () -> new UpdateByIdMapperProvider().updateById(entity),
            "version 为 null 时应抛出 ValidationException");
        assertEquals("version不能为空", exception.getMessage(), "异常消息应为 'version不能为空'");
    }

    @Test
    @DisplayName("id 与 version 均非空时生成带乐观锁条件的 SQL")
    void testSqlWithOptimisticLock() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setId(1L);
        entity.setVersion(1);
        entity.setName("shrimp");

        String sql = new UpdateByIdMapperProvider().updateById(entity);

        assertTrue(sql.contains("name = #{name}"), "SET 中应包含业务字段");
        assertTrue(sql.contains("version = version + 1"), "SET 中应包含 version 自增");
        assertTrue(sql.contains("deleted = 0"), "WHERE 中应包含逻辑删除条件");
        assertTrue(sql.endsWith("AND version = #{version}"), "SQL 应以乐观锁条件结尾");
    }

}
