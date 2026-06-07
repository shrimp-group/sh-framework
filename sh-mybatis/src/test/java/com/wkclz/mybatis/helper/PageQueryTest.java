package com.wkclz.mybatis.helper;

import com.github.pagehelper.Page;
import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.PageData;
import com.wkclz.core.base.Pageable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * PageQuery 单元测试
 * 测试 PageQuery 类的分页查询功能，特别是基于 Pageable 接口的新方法
 *
 * @author shrimp
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PageQuery 分页查询测试")
class PageQueryTest {

    @Mock
    private Pageable mockPageable;

    /**
     * 测试实体类
     */
    static class TestEntity extends BaseEntity {
        private String name;
        private Integer age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }
    }

    /**
     * 测试查询参数类
     */
    static class TestQueryParam {
        private String keyword;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }
    }

    /**
     * 测试查询参数类（实现 Pageable）
     */
    static class TestPageableQueryParam implements Pageable {
        private String keyword;
        private Long current;
        private Long size;
        private Long offset;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        @Override
        public Long getCurrent() {
            return current;
        }

        @Override
        public void setCurrent(Long current) {
            this.current = current;
        }

        @Override
        public Long getSize() {
            return size;
        }

        @Override
        public void setSize(Long size) {
            this.size = size;
        }

        @Override
        public Long getOffset() {
            return offset;
        }

        @Override
        public void setOffset(Long offset) {
            this.offset = offset;
        }
    }

    @Test
    @DisplayName("测试基于 BaseEntity 的分页查询 - 正常情况")
    void testPageWithBaseEntity() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据
            TestEntity param = new TestEntity();
            param.setCurrent(1L);
            param.setSize(10L);

            List<TestEntity> mockResult = createMockResultList(10);
            Page<TestEntity> mockPage = new Page<>();
            mockPage.addAll(mockResult);
            mockPage.setTotal(100L);

            Function<TestEntity, List<TestEntity>> mockFunction = entity -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试
            PageData<TestEntity> result = PageQuery.page(param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");
            assertEquals(10, result.getRecords().size(), "结果记录数应为 10");
            assertEquals(100L, result.getTotal(), "总记录数应为 100");

            // 验证 PageHelper 调用
            pageHelperMock.verify(() -> com.github.pagehelper.PageHelper.startPage(1, 10), times(1));
            pageHelperMock.verify(com.github.pagehelper.PageHelper::clearPage, times(1));
        }
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询 - 正常情况")
    void testPageWithPageableInterface() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据
            when(mockPageable.getCurrent()).thenReturn(2L);
            when(mockPageable.getSize()).thenReturn(20L);

            TestQueryParam param = new TestQueryParam();
            param.setKeyword("test");

            List<String> mockResult = Arrays.asList("result1", "result2", "result3");
            Page<String> mockPage = new Page<>();
            mockPage.addAll(mockResult);
            mockPage.setTotal(50L);

            Function<TestQueryParam, List<String>> mockFunction = queryParam -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试
            PageData<String> result = PageQuery.page(mockPageable, param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");
            assertEquals(3, result.getRecords().size(), "结果记录数应为 3");
            assertEquals(50L, result.getTotal(), "总记录数应为 50");

            // 验证 Pageable 的 init 方法被调用
            verify(mockPageable, times(1)).init();

            // 验证 PageHelper 调用
            pageHelperMock.verify(() -> com.github.pagehelper.PageHelper.startPage(2, 20), times(1));
            pageHelperMock.verify(com.github.pagehelper.PageHelper::clearPage, times(1));
        }
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询 - pageable 为 null")
    void testPageWithPageableNull() {
        TestQueryParam param = new TestQueryParam();
        Function<TestQueryParam, List<String>> mockFunction = queryParam -> new ArrayList<>();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PageQuery.page(null, param, mockFunction),
                "pageable 为 null 时应抛出 IllegalArgumentException"
        );

        assertEquals("Pageable cannot be null", exception.getMessage(), "异常消息应为 'Pageable cannot be null'");
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询 - param 为 null")
    void testPageWithParamNull() {
        Function<TestQueryParam, List<String>> mockFunction = queryParam -> new ArrayList<>();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PageQuery.page(mockPageable, null, mockFunction),
                "param 为 null 时应抛出 IllegalArgumentException"
        );

        assertEquals("Param cannot be null", exception.getMessage(), "异常消息应为 'Param cannot be null'");
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询 - function 为 null")
    void testPageWithFunctionNull() {
        TestQueryParam param = new TestQueryParam();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PageQuery.page(mockPageable, param, null),
                "function 为 null 时应抛出 IllegalArgumentException"
        );

        assertEquals("Function cannot be null", exception.getMessage(), "异常消息应为 'Function cannot be null'");
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询 - 分页参数自动初始化")
    void testPageWithPageableAutoInit() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据 - 模拟 init 方法设置默认值
            when(mockPageable.getCurrent()).thenReturn(null, 1L);
            when(mockPageable.getSize()).thenReturn(null, 10L);

            TestQueryParam param = new TestQueryParam();

            List<String> mockResult = Arrays.asList("result1");
            Page<String> mockPage = new Page<>();
            mockPage.addAll(mockResult);
            mockPage.setTotal(1L);

            Function<TestQueryParam, List<String>> mockFunction = queryParam -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 模拟 init 方法的行为
            doAnswer(invocation -> {
                when(mockPageable.getCurrent()).thenReturn(1L);
                when(mockPageable.getSize()).thenReturn(10L);
                return null;
            }).when(mockPageable).init();

            // 执行测试
            PageData<String> result = PageQuery.page(mockPageable, param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");

            // 验证 init 方法被调用
            verify(mockPageable, times(1)).init();
        }
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询 - 空结果")
    void testPageWithPageableEmptyResult() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据
            when(mockPageable.getCurrent()).thenReturn(1L);
            when(mockPageable.getSize()).thenReturn(10L);

            TestQueryParam param = new TestQueryParam();

            Page<String> mockPage = new Page<>();
            mockPage.setTotal(0L);

            Function<TestQueryParam, List<String>> mockFunction = queryParam -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试
            PageData<String> result = PageQuery.page(mockPageable, param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");
            assertTrue(result.getRecords().isEmpty(), "结果记录应为空");
            assertEquals(0L, result.getTotal(), "总记录数应为 0");
        }
    }

    @Test
    @DisplayName("测试 PageHelper.clearPage() 在异常情况下也会被调用")
    void testPageHelperClearPageOnException() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据
            when(mockPageable.getCurrent()).thenReturn(1L);
            when(mockPageable.getSize()).thenReturn(10L);

            TestQueryParam param = new TestQueryParam();

            // Mock 查询函数抛出异常
            Function<TestQueryParam, List<String>> mockFunction = queryParam -> {
                throw new RuntimeException("模拟查询异常");
            };

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试并验证异常
            assertThrows(RuntimeException.class, () -> PageQuery.page(mockPageable, param, mockFunction),
                    "应抛出运行时异常");

            // 验证 clearPage 被调用（finally 块确保资源清理）
            pageHelperMock.verify(com.github.pagehelper.PageHelper::clearPage, times(1));
        }
    }

    @Test
    @DisplayName("测试基于 BaseEntity 的分页查询 - 参数自动初始化")
    void testPageWithBaseEntityAutoInit() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据 - 不设置分页参数，验证自动初始化
            TestEntity param = new TestEntity();

            List<TestEntity> mockResult = createMockResultList(5);
            Page<TestEntity> mockPage = new Page<>();
            mockPage.addAll(mockResult);
            mockPage.setTotal(5L);

            Function<TestEntity, List<TestEntity>> mockFunction = entity -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试
            PageData<TestEntity> result = PageQuery.page(param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");
            assertEquals(5, result.getRecords().size(), "结果记录数应为 5");

            // 验证分页参数被自动初始化为默认值
            assertEquals(1L, param.getCurrent(), "页码应被初始化为 1");
            assertEquals(10L, param.getSize(), "分页大小应被初始化为 10");
        }
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询（分页参数与查询参数合一）- 正常情况")
    void testPageWithPageableParamCombined() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据
            TestPageableQueryParam param = new TestPageableQueryParam();
            param.setCurrent(3L);
            param.setSize(30L);
            param.setKeyword("combined");

            List<String> mockResult = Arrays.asList("result1", "result2", "result3", "result4");
            Page<String> mockPage = new Page<>();
            mockPage.addAll(mockResult);
            mockPage.setTotal(200L);

            Function<TestPageableQueryParam, List<String>> mockFunction = queryParam -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试
            PageData<String> result = PageQuery.page(param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");
            assertEquals(4, result.getRecords().size(), "结果记录数应为 4");
            assertEquals(200L, result.getTotal(), "总记录数应为 200");

            // 验证 PageHelper 调用使用了正确的分页参数
            pageHelperMock.verify(() -> com.github.pagehelper.PageHelper.startPage(3, 30), times(1));
            pageHelperMock.verify(com.github.pagehelper.PageHelper::clearPage, times(1));
        }
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询（分页参数与查询参数合一）- param 为 null")
    void testPageWithPageableParamCombinedNull() {
        Function<TestPageableQueryParam, List<String>> mockFunction = queryParam -> new ArrayList<>();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PageQuery.page((TestPageableQueryParam) null, mockFunction),
                "param 为 null 时应抛出 IllegalArgumentException"
        );

        assertEquals("Param cannot be null", exception.getMessage(), "异常消息应为 'Param cannot be null'");
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询（分页参数与查询参数合一）- function 为 null")
    void testPageWithPageableParamCombinedFunctionNull() {
        TestPageableQueryParam param = new TestPageableQueryParam();

        // 执行测试并验证异常
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> PageQuery.page(param, null),
                "function 为 null 时应抛出 IllegalArgumentException"
        );

        assertEquals("Function cannot be null", exception.getMessage(), "异常消息应为 'Function cannot be null'");
    }

    @Test
    @DisplayName("测试基于 Pageable 接口的分页查询（分页参数与查询参数合一）- 分页参数自动初始化")
    void testPageWithPageableParamCombinedAutoInit() {
        try (MockedStatic<com.github.pagehelper.PageHelper> pageHelperMock = mockStatic(com.github.pagehelper.PageHelper.class)) {
            // 准备测试数据 - 不设置分页参数，验证自动初始化
            TestPageableQueryParam param = new TestPageableQueryParam();
            param.setKeyword("auto-init");

            List<String> mockResult = Arrays.asList("result1");
            Page<String> mockPage = new Page<>();
            mockPage.addAll(mockResult);
            mockPage.setTotal(1L);

            Function<TestPageableQueryParam, List<String>> mockFunction = queryParam -> mockPage;

            // Mock PageHelper 静态方法
            pageHelperMock.when(() -> com.github.pagehelper.PageHelper.startPage(anyInt(), anyInt()))
                    .thenAnswer(invocation -> null);
            pageHelperMock.when(com.github.pagehelper.PageHelper::clearPage)
                    .thenAnswer(invocation -> null);

            // 执行测试
            PageData<String> result = PageQuery.page(param, mockFunction);

            // 验证结果
            assertNotNull(result, "分页结果不应为 null");

            // 验证分页参数被自动初始化为默认值
            assertEquals(1L, param.getCurrent(), "页码应被初始化为 1");
            assertEquals(10L, param.getSize(), "分页大小应被初始化为 10");
        }
    }

    /**
     * 创建模拟结果列表
     */
    private List<TestEntity> createMockResultList(int count) {
        List<TestEntity> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            TestEntity entity = new TestEntity();
            entity.setName("name" + i);
            entity.setAge(20 + i);
            list.add(entity);
        }
        return list;
    }
}