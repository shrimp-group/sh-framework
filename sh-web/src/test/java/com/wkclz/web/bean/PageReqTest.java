package com.wkclz.web.bean;

import com.wkclz.core.base.Pageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PageReq 单元测试
 * 测试 PageReq 实现 Pageable 接口的功能
 *
 * @author shrimp
 */
@DisplayName("PageReq 分页请求测试")
class PageReqTest {

    @Test
    @DisplayName("测试 PageReq 实现 Pageable 接口")
    void testPageReqImplementsPageable() {
        PageReq pageReq = new PageReq();

        // 验证 PageReq 实现了 Pageable 接口
        assertTrue(pageReq instanceof Pageable, "PageReq 应实现 Pageable 接口");
    }

    @Test
    @DisplayName("测试 getter 和 setter 方法")
    void testGetterAndSetter() {
        PageReq pageReq = new PageReq();

        pageReq.setCurrent(5L);
        pageReq.setSize(25L);
        pageReq.setOffset(100L);

        assertEquals(5L, pageReq.getCurrent(), "页码应为 5");
        assertEquals(25L, pageReq.getSize(), "分页大小应为 25");
        assertEquals(100L, pageReq.getOffset(), "偏移量应为 100");
    }

    @Test
    @DisplayName("测试 init() 方法 - 正常值")
    void testInitWithNormalValues() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(2L);
        pageReq.setSize(20L);

        pageReq.init();

        assertEquals(2L, pageReq.getCurrent(), "页码应保持不变");
        assertEquals(20L, pageReq.getSize(), "分页大小应保持不变");
        assertEquals(20L, pageReq.getOffset(), "偏移量应为 (2-1) * 20 = 20");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 为 null")
    void testInitWithNullCurrent() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(null);
        pageReq.setSize(15L);

        pageReq.init();

        assertEquals(1L, pageReq.getCurrent(), "null 页码应设置为默认值 1");
        assertEquals(15L, pageReq.getSize(), "分页大小应保持不变");
        assertEquals(0L, pageReq.getOffset(), "偏移量应为 (1-1) * 15 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - size 为 null")
    void testInitWithNullSize() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(3L);
        pageReq.setSize(null);

        pageReq.init();

        assertEquals(3L, pageReq.getCurrent(), "页码应保持不变");
        assertEquals(10L, pageReq.getSize(), "null 分页大小应设置为默认值 10");
        assertEquals(20L, pageReq.getOffset(), "偏移量应为 (3-1) * 10 = 20");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 和 size 都为 null")
    void testInitWithBothNull() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(null);
        pageReq.setSize(null);

        pageReq.init();

        assertEquals(1L, pageReq.getCurrent(), "null 页码应设置为默认值 1");
        assertEquals(10L, pageReq.getSize(), "null 分页大小应设置为默认值 10");
        assertEquals(0L, pageReq.getOffset(), "偏移量应为 (1-1) * 10 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 为非法值 (小于 1)")
    void testInitWithInvalidCurrent() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(0L);
        pageReq.setSize(10L);

        pageReq.init();

        assertEquals(1L, pageReq.getCurrent(), "非法页码 0 应设置为默认值 1");
        assertEquals(10L, pageReq.getSize(), "分页大小应保持不变");
        assertEquals(0L, pageReq.getOffset(), "偏移量应为 (1-1) * 10 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - size 为非法值 (小于 1)")
    void testInitWithInvalidSize() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(1L);
        pageReq.setSize(0L);

        pageReq.init();

        assertEquals(1L, pageReq.getCurrent(), "页码应保持不变");
        assertEquals(10L, pageReq.getSize(), "非法分页大小 0 应设置为默认值 10");
        assertEquals(0L, pageReq.getOffset(), "偏移量应为 (1-1) * 10 = 0");
    }

    @Test
    @DisplayName("测试 init() 方法 - current 为负数")
    void testInitWithNegativeCurrent() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(-5L);
        pageReq.setSize(10L);

        pageReq.init();

        assertEquals(1L, pageReq.getCurrent(), "负数页码应设置为默认值 1");
        assertEquals(10L, pageReq.getSize(), "分页大小应保持不变");
    }

    @Test
    @DisplayName("测试 init() 方法 - size 为负数")
    void testInitWithNegativeSize() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(1L);
        pageReq.setSize(-10L);

        pageReq.init();

        assertEquals(1L, pageReq.getCurrent(), "页码应保持不变");
        assertEquals(10L, pageReq.getSize(), "负数分页大小应设置为默认值 10");
    }

    @Test
    @DisplayName("测试偏移量计算")
    void testOffsetCalculation() {
        PageReq pageReq = new PageReq();

        // 第一页
        pageReq.setCurrent(1L);
        pageReq.setSize(10L);
        pageReq.init();
        assertEquals(0L, pageReq.getOffset(), "第一页偏移量应为 0");

        // 第二页
        pageReq.setCurrent(2L);
        pageReq.setSize(10L);
        pageReq.init();
        assertEquals(10L, pageReq.getOffset(), "第二页偏移量应为 10");

        // 第三页，每页 20 条
        pageReq.setCurrent(3L);
        pageReq.setSize(20L);
        pageReq.init();
        assertEquals(40L, pageReq.getOffset(), "第三页偏移量应为 40");
    }

    @Test
    @DisplayName("测试默认构造函数")
    void testDefaultConstructor() {
        PageReq pageReq = new PageReq();

        assertNull(pageReq.getCurrent(), "默认页码应为 null");
        assertNull(pageReq.getSize(), "默认分页大小应为 null");
        assertNull(pageReq.getOffset(), "默认偏移量应为 null");
    }

    @Test
    @DisplayName("测试多次调用 init() 方法")
    void testMultipleInitCalls() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(3L);
        pageReq.setSize(20L);

        pageReq.init();
        assertEquals(3L, pageReq.getCurrent(), "第一次 init 后页码应为 3");
        assertEquals(20L, pageReq.getSize(), "第一次 init 后分页大小应为 20");
        assertEquals(40L, pageReq.getOffset(), "第一次 init 后偏移量应为 40");

        // 修改值后再次调用 init
        pageReq.setCurrent(5L);
        pageReq.setSize(30L);
        pageReq.init();

        assertEquals(5L, pageReq.getCurrent(), "第二次 init 后页码应为 5");
        assertEquals(30L, pageReq.getSize(), "第二次 init 后分页大小应为 30");
        assertEquals(120L, pageReq.getOffset(), "第二次 init 后偏移量应为 120");
    }

    @Test
    @DisplayName("测试 PageReq 序列化能力")
    void testPageReqSerializable() {
        PageReq pageReq = new PageReq();
        pageReq.setCurrent(1L);
        pageReq.setSize(10L);

        // 验证 PageReq 实现了 Serializable 接口
        assertTrue(java.io.Serializable.class.isAssignableFrom(PageReq.class),
                "PageReq 应实现 Serializable 接口");
    }

    @Test
    @DisplayName("测试使用 Pageable 接口引用")
    void testPageableInterfaceReference() {
        Pageable pageable = new PageReq();
        pageable.setCurrent(2L);
        pageable.setSize(15L);

        pageable.init();

        assertEquals(2L, pageable.getCurrent(), "页码应为 2");
        assertEquals(15L, pageable.getSize(), "分页大小应为 15");
        assertEquals(15L, pageable.getOffset(), "偏移量应为 15");
    }
}