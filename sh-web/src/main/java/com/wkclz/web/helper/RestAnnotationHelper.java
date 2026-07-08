package com.wkclz.web.helper;

import com.alibaba.fastjson2.JSONObject;
import com.wkclz.core.annotation.Router;
import com.wkclz.tool.bean.GenericTypeInfo;
import com.wkclz.tool.utils.ClassTypeHelper;
import com.wkclz.web.bean.RestField;
import com.wkclz.web.bean.RestInfo;
import com.wkclz.web.bean.RestParam;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.*;

/**
 * REST 注解解析工具类
 * 负责解析 REST 接口的注解信息，包括 Swagger 注解、参数注解等
 *
 * @author shrimp
 */
public class RestAnnotationHelper {

    private static final Logger logger = LoggerFactory.getLogger(RestAnnotationHelper.class);

    /**
     * 提取类级别 @Tag 注解信息
     *
     * @param clazz 类对象
     * @return @Tag 的 name 和 description 组合字符串
     */
    public static String extractClassTag(Class<?> clazz) {
        Tag tag = clazz.getAnnotation(Tag.class);
        if (tag == null) {
            return null;
        }
        String name = tag.name();
        String description = tag.description();
        if (StringUtils.isBlank(name) && StringUtils.isBlank(description)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(name)) {
            sb.append(name);
        }
        if (StringUtils.isNotBlank(description)) {
            if (sb.length() > 0) {
                sb.append(" - ");
            }
            sb.append(description);
        }
        logger.debug("Class {} @Tag: {}", clazz.getName(), sb.toString());
        return sb.toString();
    }

    /**
     * 提取方法参数信息
     *
     * @param method 方法对象
     * @return 参数列表
     */
    public static List<RestParam> extractParameters(Method method) {
        List<RestParam> paramList = new ArrayList<>();
        Parameter[] parameters = method.getParameters();

        if (parameters == null || parameters.length == 0) {
            logger.debug("Method {} has no parameters", method.getName());
            return paramList;
        }

        logger.debug("Extracting parameters for method: {}", method.getName());

        for (Parameter parameter : parameters) {
            RestParam restParam = new RestParam();

            // 参数名称
            String paramName = parameter.getName();
            restParam.setName(paramName);

            // 参数类型
            Class<?> paramType = parameter.getType();
            restParam.setType(paramType.getName());

            // 获取参数注解
            Annotation[] annotations = parameter.getAnnotations();
            String annotationType = null;
            Boolean required = null;
            String defaultValue = null;
            String description = null;
            String example = null;
            String requiredMode = null;

            for (Annotation annotation : annotations) {
                // 处理 @RequestBody
                if (annotation instanceof RequestBody) {
                    RequestBody requestBody = (RequestBody) annotation;
                    annotationType = "RequestBody";
                    required = requestBody.required();
                    description = requestBody.description();
                    logger.debug("Parameter {} is annotated with @RequestBody, required: {}", paramName, required);
                    break;
                }

                // 处理 @PathVariable
                if (annotation instanceof org.springframework.web.bind.annotation.PathVariable) {
                    org.springframework.web.bind.annotation.PathVariable pathVariable = (org.springframework.web.bind.annotation.PathVariable) annotation;
                    annotationType = "PathVariable";
                    required = pathVariable.required();
                    logger.debug("Parameter {} is annotated with @PathVariable, required: {}", paramName, required);
                    break;
                }

                // 处理 @RequestParam
                if (annotation instanceof org.springframework.web.bind.annotation.RequestParam) {
                    org.springframework.web.bind.annotation.RequestParam requestParam = (org.springframework.web.bind.annotation.RequestParam) annotation;
                    annotationType = "RequestParam";
                    required = requestParam.required();
                    // 默认值
                    String defValue = requestParam.defaultValue();
                    // Spring 的默认值标记，表示没有设置默认值
                    if (defValue != null && !defValue.equals("\n\t\t\n\t\t\n\u0000\u0001\u0002\n\t\t\t\t\n")) {
                        defaultValue = defValue;
                        logger.debug("Parameter {} is annotated with @RequestParam, required: {}, defaultValue: {}", paramName, required, defaultValue);
                    } else {
                        logger.debug("Parameter {} is annotated with @RequestParam, required: {}", paramName, required);
                    }
                    break;
                }
            }

            // 处理 @Schema 注解（可以与其他注解共存）
            for (Annotation annotation : annotations) {
                if (annotation instanceof io.swagger.v3.oas.annotations.media.Schema) {
                    io.swagger.v3.oas.annotations.media.Schema schema = (io.swagger.v3.oas.annotations.media.Schema) annotation;
                    if (StringUtils.isBlank(description)) {
                        description = schema.description();
                    }
                    if (StringUtils.isBlank(example)) {
                        example = schema.example();
                    }
                    if (StringUtils.isBlank(requiredMode)) {
                        requiredMode = schema.requiredMode().name();
                    }
                    // 如果没有通过 @RequestBody/@PathVariable/@RequestParam 设置 required，
                    // 则根据 requiredMode 判断
                    if (annotationType == null && required == null) {
                        required = schema.requiredMode() == io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;
                    }
                    logger.debug("Parameter {} @Schema: description={}, example={}, requiredMode={}",
                        paramName, description, example, requiredMode);
                }
            }

            // 如果没有找到参数注解，则默认为普通参数
            if (annotationType == null) {
                annotationType = "Parameter";
                if (required == null) {
                    required = false;
                }
                logger.debug("Parameter {} has no annotation, treated as plain parameter", paramName);
            }

            restParam.setAnnotationType(annotationType);
            restParam.setRequired(required);
            restParam.setDefaultValue(defaultValue);
            restParam.setDescription(description);
            restParam.setExample(example);
            restParam.setRequiredMode(requiredMode);

            // 提取泛型类型信息
            Type genericType = parameter.getParameterizedType();
            List<String> genericTypes = ClassTypeHelper.extractGenericTypes(genericType);
            restParam.setGenericTypes(genericTypes);

            if (CollectionUtils.isNotEmpty(genericTypes)) {
                logger.debug("Parameter {} has generic types: {}", paramName, genericTypes);
            }

            // 扫描复杂类型的字段结构
            if (ClassTypeHelper.isComplexType(paramType)) {
                Set<Class<?>> scannedClasses = new HashSet<>();
                List<ClassTypeHelper.FieldInfo> fieldInfos = ClassTypeHelper.scanClassFields(paramType, genericType, 0, scannedClasses, true);
                List<RestField> fields = convertFieldInfos(fieldInfos);
                restParam.setFields(fields);
                logger.debug("Parameter {} is complex type, scanned {} fields", paramName, fields.size());
            }

            paramList.add(restParam);
        }

        logger.debug("Extracted {} parameters for method: {}", paramList.size(), method.getName());
        return paramList;
    }

    /**
     * 提取返回类型信息
     *
     * @param method   方法对象
     * @param restInfo REST 信息对象
     */
    public static void extractReturnType(Method method, RestInfo restInfo) {
        // 获取返回类型
        Class<?> returnType = method.getReturnType();

        // 处理 void 返回类型
        if (returnType == void.class || returnType == Void.class) {
            restInfo.setReturnType("void");
            restInfo.setReturnGenericInfo(null);
            restInfo.setReturnSchema(null);
            return;
        }

        // 设置返回类型
        restInfo.setReturnType(returnType.getName());

        // 获取返回类型的泛型信息
        Type genericReturnType = method.getGenericReturnType();

        // 解析泛型信息
        GenericTypeInfo genericTypeInfo = ClassTypeHelper.parseGenericType(genericReturnType);

        // 如果有泛型信息，则序列化为 JSON
        if (genericTypeInfo != null && CollectionUtils.isNotEmpty(genericTypeInfo.getTypeArgs())) {
            restInfo.setReturnGenericInfo(JSONObject.toJSONString(genericTypeInfo));
        } else {
            restInfo.setReturnGenericInfo(null);
        }

        // 提取返回值结构
        extractReturnSchema(method, restInfo, genericReturnType);
    }

    /**
     * 提取返回值完整结构
     *
     * @param method             方法对象
     * @param restInfo           REST 信息对象
     * @param genericReturnType 泛型返回类型
     */
    public static void extractReturnSchema(Method method, RestInfo restInfo, Type genericReturnType) {
        if (genericReturnType == null) {
            return;
        }

        // 解析泛型类型，找到实际的业务类型
        Class<?> actualClass = ClassTypeHelper.resolveActualClass(genericReturnType);
        if (actualClass == null || ClassTypeHelper.isSimpleType(actualClass)) {
            return;
        }

        // 扫描返回值的字段结构
        Set<Class<?>> scannedClasses = new HashSet<>();
        List<ClassTypeHelper.FieldInfo> fieldInfos = ClassTypeHelper.scanClassFields(actualClass, genericReturnType, 0, scannedClasses, true);
        List<RestField> fields = convertFieldInfos(fieldInfos);

        if (CollectionUtils.isNotEmpty(fields)) {
            restInfo.setReturnSchema(JSONObject.toJSONString(fields));
            logger.debug("Method {} return schema extracted: {} fields", method.getName(), fields.size());
        }
    }

    /**
     * 将 ClassTypeHelper.FieldInfo 列表转换为 RestField 列表
     */
    public static List<RestField> convertFieldInfos(List<ClassTypeHelper.FieldInfo> fieldInfos) {
        List<RestField> fields = new ArrayList<>();
        if (CollectionUtils.isEmpty(fieldInfos)) {
            return fields;
        }

        for (ClassTypeHelper.FieldInfo fieldInfo : fieldInfos) {
            RestField restField = new RestField();
            restField.setName(fieldInfo.getName());
            restField.setType(fieldInfo.getType());
            restField.setSimpleType(fieldInfo.getSimpleType());
            restField.setDescription(fieldInfo.getDescription());
            restField.setExample(fieldInfo.getExample());
            restField.setRequired(fieldInfo.getRequired());
            restField.setGenericTypes(fieldInfo.getGenericTypes());
            restField.setSelfReferencing(fieldInfo.getSelfReferencing());

            // 递归转换子字段
            if (CollectionUtils.isNotEmpty(fieldInfo.getFields())) {
                restField.setFields(convertFieldInfos(fieldInfo.getFields()));
            }

            fields.add(restField);
        }

        return fields;
    }

    /**
     * 从 Router 类中提取描述信息并填充到 RestInfo 中
     *
     * @param routerClassList Router 类列表
     * @param rests           REST 信息列表
     */
    public static void appendDesc(List<Class<?>> routerClassList, List<RestInfo> rests) {
        if (CollectionUtils.isEmpty(routerClassList) || CollectionUtils.isEmpty(rests)) {
            return;
        }

        for (Class<?> routerClazz : routerClassList) {
            try {
                Router routerAnno = routerClazz.getAnnotation(Router.class);
                String module = null;
                if (routerAnno != null) {
                    module = routerAnno.module();
                }

                // 填充 module
                if (module != null) {
                    String routerPackage = routerClazz.getPackageName();
                    for (RestInfo rest : rests) {
                        if (rest.getClazz() == null) {
                            continue;
                        }
                        String restPackage = rest.getClazz().getPackageName();
                        if (restPackage.startsWith(routerPackage)) {
                            rest.setModule(module);
                            rest.setAppCode(module);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
