package com.wkclz.tool.utils;

import com.wkclz.tool.bean.GenericTypeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 类类型解析工具类
 * 提供通用的反射和类型解析功能
 * 
 * @author shrimp
 */
public class ClassTypeHelper {

    private static final Logger logger = LoggerFactory.getLogger(ClassTypeHelper.class);

    /**
     * 扫描深度限制，防止无限递归
     */
    private static final int MAX_SCAN_DEPTH = 10;

    /**
     * 判断是否为简单类型
     * 简单类型包括：基本类型、String、Date、Number、Boolean 等
     *
     * @param clazz 类对象
     * @return true 表示简单类型，false 表示复杂类型
     */
    public static boolean isSimpleType(Class<?> clazz) {
        if (clazz == null) {
            return true;
        }
        // 基本类型
        if (clazz.isPrimitive()) {
            return true;
        }
        // 包装类型
        if (clazz == Boolean.class || clazz == Byte.class ||
            clazz == Character.class || clazz == Short.class ||
            clazz == Integer.class || clazz == Long.class ||
            clazz == Float.class || clazz == Double.class ||
            clazz == Void.class) {
            return true;
        }
        // 常见简单类型
        String className = clazz.getName();
        return className.startsWith("java.lang.") ||
               className.startsWith("java.math.") ||
               className.startsWith("java.time.") ||
               className.equals("java.util.Date") ||
               className.equals("java.sql.Date") ||
               className.equals("java.sql.Timestamp") ||
               className.equals("java.util.Optional");
    }

    /**
     * 判断是否为复杂类型（需要递归扫描字段的类型）
     *
     * @param clazz 类对象
     * @return true 表示复杂类型，false 表示简单类型
     */
    public static boolean isComplexType(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }
        // 简单类型不需要扫描
        if (isSimpleType(clazz)) {
            return false;
        }
        // 数组类型
        if (clazz.isArray()) {
            return isComplexType(clazz.getComponentType());
        }
        // 集合类型不直接扫描，但会扫描其泛型参数
        if (Iterable.class.isAssignableFrom(clazz) || Map.class.isAssignableFrom(clazz)) {
            return false;
        }
        // 枚举类型
        if (clazz.isEnum()) {
            return false;
        }
        return true;
    }

    /**
     * 提取泛型类型信息
     *
     * @param type 类型对象
     * @return 泛型类型列表
     */
    public static List<String> extractGenericTypes(Type type) {
        List<String> genericTypes = new ArrayList<>();

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                for (Type actualType : actualTypeArguments) {
                    if (actualType instanceof Class) {
                        genericTypes.add(((Class<?>) actualType).getName());
                    } else if (actualType instanceof ParameterizedType) {
                        // 处理嵌套泛型，如 List<Map<String, Object>>
                        genericTypes.add(actualType.getTypeName());
                    } else {
                        genericTypes.add(actualType.getTypeName());
                    }
                }
            }
        }

        return genericTypes;
    }

    /**
     * 递归解析泛型类型信息
     *
     * @param type 类型对象
     * @return 泛型类型信息
     */
    public static GenericTypeInfo parseGenericType(Type type) {
        if (type == null) {
            return null;
        }

        // 处理 ParameterizedType（泛型类型）
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type rawType = parameterizedType.getRawType();
            Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();

            GenericTypeInfo info = new GenericTypeInfo();

            // 设置原始类型
            if (rawType instanceof Class) {
                info.setRawType(((Class<?>) rawType).getName());
            } else {
                info.setRawType(rawType.getTypeName());
            }

            // 递归解析类型参数
            List<GenericTypeInfo> typeArgs = new ArrayList<>();
            if (actualTypeArguments != null && actualTypeArguments.length > 0) {
                for (Type actualType : actualTypeArguments) {
                    GenericTypeInfo argInfo = parseGenericType(actualType);
                    if (argInfo != null) {
                        typeArgs.add(argInfo);
                    }
                }
            }
            info.setTypeArgs(typeArgs);

            return info;
        }

        // 处理 Class（普通类）
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            GenericTypeInfo info = new GenericTypeInfo();
            info.setRawType(clazz.getName());
            info.setTypeArgs(new ArrayList<>());
            return info;
        }

        // 处理 TypeVariable（类型变量，如 T, E 等）
        if (type instanceof TypeVariable) {
            TypeVariable<?> typeVariable = (TypeVariable<?>) type;
            GenericTypeInfo info = new GenericTypeInfo();
            info.setRawType(typeVariable.getName());
            info.setTypeArgs(new ArrayList<>());
            return info;
        }

        // 处理 WildcardType（通配符类型，如 ? extends Number）
        if (type instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) type;
            GenericTypeInfo info = new GenericTypeInfo();
            info.setRawType(wildcardType.getTypeName());
            info.setTypeArgs(new ArrayList<>());
            return info;
        }

        // 其他类型，返回 null
        return null;
    }

    /**
     * 从泛型类型中解析出实际的业务类
     * 例如：R<PageData<UserResp>> -> PageData<UserResp> -> UserResp
     *       R<UserResp> -> UserResp
     *
     * @param type 泛型类型
     * @return 实际的业务类，如果没有找到返回 null
     */
    public static Class<?> resolveActualClass(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type rawType = pt.getRawType();
            String rawTypeName = rawType instanceof Class ? ((Class<?>) rawType).getName() : rawType.getTypeName();

            // 跳过框架类型，获取内层泛型
            if (rawTypeName.startsWith("com.wkclz.") ||
                rawTypeName.equals("R") ||
                rawTypeName.equals("com.wkclz.core.base.R")) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs != null && typeArgs.length > 0) {
                    // 取最后一个泛型参数（通常是业务类型）
                    return resolveActualClass(typeArgs[typeArgs.length - 1]);
                }
            }

            // PageData, List 等容器类型
            if (rawTypeName.contains("PageData") ||
                rawTypeName.contains("List") ||
                rawTypeName.contains("ArrayList")) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs != null && typeArgs.length > 0) {
                    return resolveActualClass(typeArgs[0]);
                }
            }

            // Map 类型
            if (rawTypeName.contains("Map") || rawTypeName.contains("HashMap")) {
                Type[] typeArgs = pt.getActualTypeArguments();
                if (typeArgs != null && typeArgs.length > 1) {
                    // Map 的第二个泛型参数是 value 类型
                    return resolveActualClass(typeArgs[1]);
                }
            }

            // 如果是 Class 类型，直接返回
            if (rawType instanceof Class) {
                return (Class<?>) rawType;
            }
        }

        if (type instanceof Class) {
            return (Class<?>) type;
        }

        return null;
    }

    /**
     * 递归扫描类的字段结构（不带注解处理，通用版本）
     *
     * @param clazz 类对象
     * @param genericType 泛型类型（可能包含泛型参数信息）
     * @param depth 当前深度
     * @param scannedClasses 已扫描过的类集合（用于检测循环引用）
     * @return 字段列表
     */
    public static List<FieldInfo> scanClassFields(Class<?> clazz, Type genericType, int depth, Set<Class<?>> scannedClasses) {
        return scanClassFields(clazz, genericType, depth, scannedClasses, false);
    }

    /**
     * 递归扫描类的字段结构
     *
     * @param clazz 类对象
     * @param genericType 泛型类型（可能包含泛型参数信息）
     * @param depth 当前深度
     * @param scannedClasses 已扫描过的类集合（用于检测循环引用）
     * @param withAnnotations 是否处理注解（@Schema, @NotNull 等）
     * @return 字段列表
     */
    public static List<FieldInfo> scanClassFields(Class<?> clazz, Type genericType, int depth, Set<Class<?>> scannedClasses, boolean withAnnotations) {
        List<FieldInfo> fields = new ArrayList<>();

        if (clazz == null || depth > MAX_SCAN_DEPTH) {
            return fields;
        }

        // 检测循环引用
        if (scannedClasses.contains(clazz)) {
            logger.debug("Class {} already scanned, skipping to prevent infinite recursion", clazz.getName());
            FieldInfo selfRef = new FieldInfo();
            selfRef.setName("*self-reference*");
            selfRef.setType(clazz.getName());
            selfRef.setSelfReferencing(true);
            fields.add(selfRef);
            return fields;
        }

        // 获取类的所有字段（包括父类的字段）
        List<Field> allFields = new ArrayList<>();
        Class<?> currentClass = clazz;
        while (currentClass != null && currentClass != Object.class) {
            for (Field field : currentClass.getDeclaredFields()) {
                // 跳过静态字段和 transient 字段
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    java.lang.reflect.Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                allFields.add(field);
            }
            currentClass = currentClass.getSuperclass();
        }

        // 解析泛型类型，获取字段名到泛型参数的映射
        Map<String, Type> genericMap = getGenericTypeMap(genericType);

        for (Field field : allFields) {
            FieldInfo fieldInfo = new FieldInfo();
            fieldInfo.setName(field.getName());

            Class<?> fieldType = field.getType();
            fieldInfo.setType(fieldType.getName());
            fieldInfo.setSimpleType(isSimpleType(fieldType));

            // 获取字段的泛型类型
            Type fieldGenericType = field.getGenericType();
            Type resolvedType = genericMap.get(field.getName());
            if (resolvedType != null) {
                fieldGenericType = resolvedType;
            }

            // 提取泛型参数
            List<String> genericTypes = extractGenericTypes(fieldGenericType);
            fieldInfo.setGenericTypes(genericTypes);

            // 处理注解（仅在 withAnnotations 为 true 时）
            if (withAnnotations) {
                processFieldAnnotations(field, fieldInfo);
            }

            // 递归扫描复杂类型的子字段
            if (isComplexType(fieldType) && !Boolean.TRUE.equals(fieldInfo.getSelfReferencing())) {
                scannedClasses.add(clazz);
                List<FieldInfo> subFields = scanClassFields(fieldType, fieldGenericType, depth + 1, scannedClasses, withAnnotations);
                fieldInfo.setFields(subFields);
            }

            fields.add(fieldInfo);
        }

        return fields;
    }

    /**
     * 使用反射处理字段注解（@Schema, @NotNull, @NotBlank）
     */
    @SuppressWarnings("unchecked")
    private static void processFieldAnnotations(Field field, FieldInfo fieldInfo) {
        try {
            // 处理 @Schema 注解
            Class<?> schemaClass = Class.forName("io.swagger.v3.oas.annotations.media.Schema");
            Object schema = field.getAnnotation((Class<? extends Annotation>) schemaClass);
            if (schema != null) {
                java.lang.reflect.Method descMethod = schemaClass.getMethod("description");
                String description = (String) descMethod.invoke(schema);
                if (description != null && !description.isEmpty()) {
                    fieldInfo.setDescription(description);
                }

                java.lang.reflect.Method exampleMethod = schemaClass.getMethod("example");
                String example = (String) exampleMethod.invoke(schema);
                if (example != null && !example.isEmpty()) {
                    fieldInfo.setExample(example);
                }

                java.lang.reflect.Method requiredModeMethod = schemaClass.getMethod("requiredMode");
                Object requiredMode = requiredModeMethod.invoke(schema);
                if (requiredMode != null) {
                    java.lang.reflect.Method nameMethod = requiredMode.getClass().getMethod("name");
                    String modeName = (String) nameMethod.invoke(requiredMode);
                    if ("REQUIRED".equals(modeName)) {
                        fieldInfo.setRequired(true);
                    }
                }
            }
        } catch (Exception e) {
            // Schema 注解不存在，忽略
        }

        try {
            // 处理 @NotNull 注解
            Class<?> notNullClass = Class.forName("jakarta.validation.constraints.NotNull");
            if (field.isAnnotationPresent((Class<? extends Annotation>) notNullClass)) {
                fieldInfo.setRequired(true);
            }
        } catch (Exception e) {
            // NotNull 注解不存在，忽略
        }

        try {
            // 处理 @NotBlank 注解
            Class<?> notBlankClass = Class.forName("jakarta.validation.constraints.NotBlank");
            if (field.isAnnotationPresent((Class<? extends Annotation>) notBlankClass)) {
                fieldInfo.setRequired(true);
            }
        } catch (Exception e) {
            // NotBlank 注解不存在，忽略
        }
    }

    /**
     * 从泛型类型中提取字段名到类型的映射
     */
    private static Map<String, Type> getGenericTypeMap(Type genericType) {
        // 简化实现：返回空 map，高级泛型映射需要在调用处传递
        return Map.of();
    }

    /**
     * 字段信息类
     * 用于描述类的字段结构
     */
    @lombok.Data
    public static class FieldInfo {
        /**
         * 字段名称
         */
        private String name;

        /**
         * 字段类型（完整类名）
         */
        private String type;

        /**
         * 是否为简单类型
         */
        private Boolean simpleType;

        /**
         * 字段描述
         */
        private String description;

        /**
         * 示例值
         */
        private String example;

        /**
         * 是否必填
         */
        private Boolean required;

        /**
         * 泛型参数类型列表
         */
        private List<String> genericTypes;

        /**
         * 子字段列表
         */
        private List<FieldInfo> fields;

        /**
         * 是否为自引用类型
         */
        private Boolean selfReferencing;

        public FieldInfo() {
            this.genericTypes = new ArrayList<>();
            this.fields = new ArrayList<>();
        }
    }

}
