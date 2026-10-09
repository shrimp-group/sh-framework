package com.wkclz.mybatis.mapper.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SelectByEntityMapperProvider 实体条件查询 SQL 生成测试
 *
 * @author shrimp
 */
@DisplayName("SelectByEntityMapperProvider 实体条件查询 SQL 生成测试")
class SelectByEntityMapperProviderTest {

    @Test
    @DisplayName("ids 非空时生成 id IN 条件")
    void testIdsNotEmpty() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setName("shrimp");
        entity.setIds(List.of(1L, 2L, 3L));

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertTrue(sql.contains("AND id IN (#{ids[0]}, #{ids[1]}, #{ids[2]})"), "WHERE 中应包含 id IN 条件");
        assertTrue(sql.contains("deleted = 0"), "WHERE 中应包含逻辑删除条件");
        assertTrue(sql.contains("name = #{name}"), "WHERE 中应包含业务字段条件");
    }

    @Test
    @DisplayName("ids 为 null 时不生成 id IN 条件")
    void testIdsNull() throws IllegalAccessException {
        TestEntity entity = new TestEntity();

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertFalse(sql.contains("id IN"), "不应包含 id IN 条件");
        assertTrue(sql.contains("WHERE deleted = 0 ORDER BY"), "无条件时 WHERE 应仅包含逻辑删除条件");
    }

    @Test
    @DisplayName("ids 为空集合时不生成 id IN 条件")
    void testIdsEmpty() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setIds(List.of());

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertFalse(sql.contains("id IN"), "不应包含 id IN 条件，避免生成非法 SQL IN ()");
    }

    @Test
    @DisplayName("仅 timeFrom 非空时生成 create_time >= 条件")
    void testTimeFromOnly() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setTimeFrom(LocalDateTime.of(2026, 1, 1, 0, 0));

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertTrue(sql.contains("AND create_time >= #{timeFrom}"), "应包含 create_time >= 条件");
        assertFalse(sql.contains("create_time <="), "不应包含 create_time <= 条件");
    }

    @Test
    @DisplayName("仅 timeTo 非空时生成 create_time <= 条件")
    void testTimeToOnly() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setTimeTo(LocalDateTime.of(2026, 1, 31, 23, 59, 59));

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertTrue(sql.contains("AND create_time <= #{timeTo}"), "应包含 create_time <= 条件");
        assertFalse(sql.contains("create_time >="), "不应包含 create_time >= 条件");
    }

    @Test
    @DisplayName("timeFrom 与 timeTo 均非空时生成范围条件")
    void testTimeRangeBoth() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setTimeFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        entity.setTimeTo(LocalDateTime.of(2026, 1, 31, 23, 59, 59));

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertTrue(sql.contains("AND create_time >= #{timeFrom}"), "应包含 create_time >= 条件");
        assertTrue(sql.contains("AND create_time <= #{timeTo}"), "应包含 create_time <= 条件");
    }

    @Test
    @DisplayName("timeFrom 与 timeTo 均为空时不生成范围条件")
    void testTimeRangeNone() throws IllegalAccessException {
        TestEntity entity = new TestEntity();

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertFalse(sql.contains("create_time >="), "不应包含 create_time >= 条件");
        assertFalse(sql.contains("create_time <="), "不应包含 create_time <= 条件");
    }

    @Test
    @DisplayName("ids 与时间范围组合场景")
    void testIdsWithTimeRange() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setIds(List.of(1L, 2L));
        entity.setTimeFrom(LocalDateTime.of(2026, 1, 1, 0, 0));
        entity.setTimeTo(LocalDateTime.of(2026, 1, 31, 23, 59, 59));

        String sql = new SelectByEntityMapperProvider().selectByEntity(entity);

        assertTrue(sql.contains("AND id IN (#{ids[0]}, #{ids[1]})"), "应包含 id IN 条件");
        assertTrue(sql.contains("AND create_time >= #{timeFrom}"), "应包含 create_time >= 条件");
        assertTrue(sql.contains("AND create_time <= #{timeTo}"), "应包含 create_time <= 条件");
    }

    @Test
    @DisplayName("SelectCountByEntity 携带 ids 时同样生成 id IN 条件（分页一致性）")
    void testCountWithIds() throws IllegalAccessException {
        TestEntity entity = new TestEntity();
        entity.setIds(List.of(1L, 2L));

        String sql = new SelectCountByEntityMapperProvider().selectCountByEntity(entity);

        assertTrue(sql.contains("AND id IN (#{ids[0]}, #{ids[1]})"), "COUNT SQL 应包含与查询一致的 id IN 条件");
    }

}
