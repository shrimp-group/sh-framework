package com.wkclz.web.helper;

import com.wkclz.core.base.R;
import com.wkclz.web.bean.RestInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RestHelper 返回类型提取测试
 *
 * @author shrimp
 */
class RestHelperTest {

    /**
     * 测试方法：返回单层泛型 R<String>
     */
    public R<String> testSingleGeneric() {
        return R.ok("test");
    }

    /**
     * 测试方法：返回多层嵌套泛型 R<List<String>>
     */
    public R<List<String>> testNestedGeneric() {
        return R.ok(List.of("test1", "test2"));
    }

    /**
     * 测试方法：返回三层嵌套泛型 R<Map<String, List<Integer>>>
     */
    public R<Map<String, List<Integer>>> testDeepNestedGeneric() {
        return R.ok(Map.of());
    }

    /**
     * 测试方法：返回普通类 String
     */
    public String testPlainClass() {
        return "test";
    }

    /**
     * 测试方法：返回 void
     */
    public void testVoid() {
        // void 方法
    }

    @Test
    void testExtractReturnType() throws Exception {
        // 测试单层泛型 R<String>
        Method singleGenericMethod = RestHelperTest.class.getMethod("testSingleGeneric");
        RestInfo singleGenericInfo = new RestInfo();
        invokeExtractReturnType(singleGenericMethod, singleGenericInfo);

        assertNotNull(singleGenericInfo.getReturnType());
        assertEquals(R.class.getName(), singleGenericInfo.getReturnType());
        assertNotNull(singleGenericInfo.getReturnGenericInfo());
        System.out.println("单层泛型 R<String>:");
        System.out.println("  returnType: " + singleGenericInfo.getReturnType());
        System.out.println("  returnGenericInfo: " + singleGenericInfo.getReturnGenericInfo());

        // 测试多层嵌套泛型 R<List<String>>
        Method nestedGenericMethod = RestHelperTest.class.getMethod("testNestedGeneric");
        RestInfo nestedGenericInfo = new RestInfo();
        invokeExtractReturnType(nestedGenericMethod, nestedGenericInfo);

        assertNotNull(nestedGenericInfo.getReturnType());
        assertEquals(R.class.getName(), nestedGenericInfo.getReturnType());
        assertNotNull(nestedGenericInfo.getReturnGenericInfo());
        System.out.println("\n多层嵌套泛型 R<List<String>>:");
        System.out.println("  returnType: " + nestedGenericInfo.getReturnType());
        System.out.println("  returnGenericInfo: " + nestedGenericInfo.getReturnGenericInfo());

        // 测试三层嵌套泛型 R<Map<String, List<Integer>>>
        Method deepNestedGenericMethod = RestHelperTest.class.getMethod("testDeepNestedGeneric");
        RestInfo deepNestedGenericInfo = new RestInfo();
        invokeExtractReturnType(deepNestedGenericMethod, deepNestedGenericInfo);

        assertNotNull(deepNestedGenericInfo.getReturnType());
        assertEquals(R.class.getName(), deepNestedGenericInfo.getReturnType());
        assertNotNull(deepNestedGenericInfo.getReturnGenericInfo());
        System.out.println("\n三层嵌套泛型 R<Map<String, List<Integer>>>:");
        System.out.println("  returnType: " + deepNestedGenericInfo.getReturnType());
        System.out.println("  returnGenericInfo: " + deepNestedGenericInfo.getReturnGenericInfo());

        // 测试普通类 String
        Method plainClassMethod = RestHelperTest.class.getMethod("testPlainClass");
        RestInfo plainClassInfo = new RestInfo();
        invokeExtractReturnType(plainClassMethod, plainClassInfo);

        assertNotNull(plainClassInfo.getReturnType());
        assertEquals(String.class.getName(), plainClassInfo.getReturnType());
        assertNull(plainClassInfo.getReturnGenericInfo());
        System.out.println("\n普通类 String:");
        System.out.println("  returnType: " + plainClassInfo.getReturnType());
        System.out.println("  returnGenericInfo: " + plainClassInfo.getReturnGenericInfo());

        // 测试 void
        Method voidMethod = RestHelperTest.class.getMethod("testVoid");
        RestInfo voidInfo = new RestInfo();
        invokeExtractReturnType(voidMethod, voidInfo);

        assertEquals("void", voidInfo.getReturnType());
        assertNull(voidInfo.getReturnGenericInfo());
        System.out.println("\nvoid 方法:");
        System.out.println("  returnType: " + voidInfo.getReturnType());
        System.out.println("  returnGenericInfo: " + voidInfo.getReturnGenericInfo());
    }

    /**
     * 反射调用 extractReturnType 方法
     */
    private void invokeExtractReturnType(Method method, RestInfo restInfo) throws Exception {
        java.lang.reflect.Method extractMethod = RestHelper.class.getDeclaredMethod(
            "extractReturnType", Method.class, RestInfo.class);
        extractMethod.setAccessible(true);
        extractMethod.invoke(null, method, restInfo);
    }
}