package com.wkclz.core.base;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pageable 接口单元测试
 * 测试 Pageable 接口的默认方法以及 BaseEntity 实现的兼容性
 *
 * @author shrimp
 */
@DisplayName("Pageable 接口测试")
class PageableTest {

    @Test
    @DisplayName("测试默认常量值")
    void testDefaultConstants() {
        assertEquals(1L, Pageable.DEFAULT_CURRENT, "默认页码应为 1");
        assertEquals(10L, Pageable.DEFAULT_SIZE, "默认分页大小应为 10");
    }

    @Test
    @DisplayName("测试 init() 方法 - 正常值")
    void testInitWithNormalValues() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(2L);
        entity.setSize(20L);

        entity.init();

        assertEquals(2L, entity.getCurrent(), "页码应保持不变");
        assertEquals(20L, entity.getSize(), "分页大小应保持不变");
        assertEquals(20L, entity.getOffset(), "偏移量应为 (2-1) * 20 = 20");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 为 null")
    void testInitWithNullCurrent() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(null);
        entity.setSize(15L);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "null 页码应设置为默认值 1");
        assertEquals(15L, entity.getSize(), "分页大小应保持不变");
        assertEquals(0L, entity.getOffset(), "偏移量应为 (1-1) * 15 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - size 为 null")
    void testInitWithNullSize() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(3L);
        entity.setSize(null);

        entity.init();

        assertEquals(3L, entity.getCurrent(), "页码应保持不变");
        assertEquals(10L, entity.getSize(), "null 分页大小应设置为默认值 10");
        assertEquals(20L, entity.getOffset(), "偏移量应为 (3-1) * 10 = 20");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 和 size 都为 null")
    void testInitWithBothNull() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(null);
        entity.setSize(null);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "null 页码应设置为默认值 1");
        assertEquals(10L, entity.getSize(), "null 分页大小应设置为默认值 10");
        assertEquals(0L, entity.getOffset(), "偏移量应为 (1-1) * 10 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 为非法值 (小于 1)")
    void testInitWithInvalidCurrent() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(0L);
        entity.setSize(10L);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "非法页码 0 应设置为默认值 1");
        assertEquals(10L, entity.getSize(), "分页大小应保持不变");
        assertEquals(0L, entity.getOffset(), "偏移量应为 (1-1) * 10 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - size 为非法值 (小于 1)")
    void testInitWithInvalidSize() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(1L);
        entity.setSize(0L);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "页码应保持不变");
        assertEquals(10L, entity.getSize(), "非法分页大小 0 应设置为默认值 10");
        assertEquals(0L, entity.getOffset(), "偏移量应为 (1-1) * 10 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 为负数")
    void testInitWithNegativeCurrent() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(-5L);
        entity.setSize(10L);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "负数页码应设置为默认值 1");
        assertEquals(10L, entity.getSize(), "分页大小应保持不变");
    }

    @Test
    @DisplayName("测试 init() 方法 - size 为负数")
    void testInitWithNegativeSize() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(1L);
        entity.setSize(-10L);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "页码应保持不变");
        assertEquals(10L, entity.getSize(), "负数分页大小应设置为默认值 10");
    }

    @Test
    @DisplayName("测试偏移量计算 - 第一页")
    void testOffsetCalculationFirstPage() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(1L);
        entity.setSize(10L);

        entity.init();

        assertEquals(0L, entity.getOffset(), "第一页偏移量应为 0");
    }

    @Test
    @DisplayName("测试偏移量计算 - 第二页")
    void testOffsetCalculationSecondPage() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(2L);
        entity.setSize(10L);

        entity.init();

        assertEquals(10L, entity.getOffset(), "第二页偏移量应为 10");
    }

    @Test
    @DisplayName("测试偏移量计算 - 大页码")
    void testOffsetCalculationLargePage() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(100L);
        entity.setSize(50L);

        entity.init();

        assertEquals(4950L, entity.getOffset(), "第100页偏移量应为 (100-1) * 50 = 4950");
    }

    @Test
    @DisplayName("测试 BaseEntity 实现 Pageable 接口")
    void testBaseEntityImplementsPageable() {
        BaseEntity entity = new BaseEntity();

        // 验证 BaseEntity 实现了 Pageable 接口
        assertTrue(entity instanceof Pageable, "BaseEntity 应实现 Pageable 接口");

        // 验证 getter 和 setter 方法
        entity.setCurrent(5L);
        entity.setSize(25L);
        entity.setOffset(100L);

        assertEquals(5L, entity.getCurrent(), "页码应为 5");
        assertEquals(25L, entity.getSize(), "分页大小应为 25");
        assertEquals(100L, entity.getOffset(), "偏移量应为 100");
    }

    @Test
    @DisplayName("测试多次调用 init() 方法")
    void testMultipleInitCalls() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(3L);
        entity.setSize(20L);

        entity.init();
        assertEquals(3L, entity.getCurrent(), "第一次 init 后页码应为 3");
        assertEquals(20L, entity.getSize(), "第一次 init 后分页大小应为 20");
        assertEquals(40L, entity.getOffset(), "第一次 init 后偏移量应为 40");

        // 修改值后再次调用 init
        entity.setCurrent(5L);
        entity.setSize(30L);
        entity.init();

        assertEquals(5L, entity.getCurrent(), "第二次 init 后页码应为 5");
        assertEquals(30L, entity.getSize(), "第二次 init 后分页大小应为 30");
        assertEquals(120L, entity.getOffset(), "第二次 init 后偏移量应为 120");
    }

    @Test
    @DisplayName("测试边界值 - 最大页码")
    void testMaxPageValue() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(Long.MAX_VALUE);
        entity.setSize(1L);

        entity.init();

        assertEquals(Long.MAX_VALUE, entity.getCurrent(), "最大页码应保持不变");
        assertEquals(1L, entity.getSize(), "分页大小应保持不变");
        assertEquals(Long.MAX_VALUE - 1, entity.getOffset(), "偏移量应为 Long.MAX_VALUE - 1");
    }

    @Test
    @DisplayName("测试边界值 - 最大分页大小")
    void testMaxSizeValue() {
        BaseEntity entity = new BaseEntity();
        entity.setCurrent(1L);
        entity.setSize(Long.MAX_VALUE);

        entity.init();

        assertEquals(1L, entity.getCurrent(), "页码应保持不变");
        assertEquals(Long.MAX_VALUE, entity.getSize(), "最大分页大小应保持不变");
        assertEquals(0L, entity.getOffset(), "第一页偏移量应为 0");
    }
}