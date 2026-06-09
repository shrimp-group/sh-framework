package com.wkclz.web.helper;

import com.alibaba.fastjson2.JSONObject;
import com.wkclz.core.annotation.ApiDesc;
import com.wkclz.core.annotation.Desc;
import com.wkclz.core.annotation.Router;
import com.wkclz.tool.utils.ClassUtil;
import com.wkclz.tool.utils.StringUtil;
import com.wkclz.web.bean.RestInfo;
import com.wkclz.web.bean.RestParam;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author shrimp
 */
public class RestHelper {

    private static final Logger logger = LoggerFactory.getLogger(RestHelper.class);

    /**
     * 获取所有接口字符串
     */
    public static String getMappingStr(String packagePath, String appCode, String filter) {
        List<RestInfo> mappings = getMapping(appCode, packagePath, filter);
        return JSONObject.toJSONString(mappings);
    }


    public static List<RestInfo> getMapping() {
        return getMapping(null, null, null);
    }
    public static List<RestInfo> getMapping(String packagePath) {
        return getMapping(packagePath, null, null);
    }

    public static List<RestInfo> getMapping(String packagePath, String appCode) {
        return getMapping(packagePath, appCode, null);
    }

    public static List<RestInfo> getMapping(String packagePath, String appCode, String filter) {


        if (packagePath == null) {
            // 获取二级域下的所有 Class
            String clazzName = RestHelper.class.getName();
            int index = clazzName.indexOf(".", clazzName.indexOf(".") + 1);
            packagePath = clazzName.substring(0, index);
        }

        List<RestInfo> rests = new ArrayList<>();
        logger.info("package {} mappings...", packagePath);

        // 筛选出有 Controller 标识的类
        Set<Class<?>> classes = ClassUtil.getClasses(packagePath);
        // Rest 服务类
        List<Class<?>> restClassList = classes.stream().filter(clazz -> clazz.isAnnotationPresent(RestController.class) || clazz.isAnnotationPresent(Controller.class)).toList();
        List<Class<?>> routerClassList = classes.stream().filter(clazz -> clazz.isAnnotationPresent(Router.class)).toList();
        for (Class<?> clazz : restClassList) {
            // 大 Rest 上的 RequestMapping
            String prefix = null;
            boolean hasPreFix = clazz.isAnnotationPresent(RequestMapping.class);
            if (hasPreFix) {
                RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                String[] values = annotation.value();
                if (values.length > 0) {
                    prefix = values[0];
                }
            }
            if (prefix != null && !prefix.startsWith("/")) {
                prefix = "/" + prefix;
            }
            if (prefix != null && prefix.endsWith("/")) {
                prefix = prefix.substring(0, prefix.length() - 1);
            }

            // 获取类上的方法
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {
                RestInfo rest = getRest(method, prefix);
                if (rest != null) {
                    rest.setClazz(clazz);
                    rests.add(rest);
                }
            }
        }

        appendDesc(routerClassList, rests);

        if (StringUtils.isNotBlank(filter)) {
            rests = rests.stream().filter(t -> t.getUri().contains(filter)).toList();
        }
        if (StringUtils.isNotBlank(appCode)) {
            rests.forEach(t -> t.setAppCode(appCode));
        }
        return rests;
    }

    private static RestInfo getRest(Method method, String prefix) {
        if (method == null) {
            return null;
        }
        Annotation[] annotations = method.getAnnotations();
        String uri = null;
        String desc = null;
        RequestMethod requestMethod = null;
        for (Annotation annotation : annotations) {
            if (RequestMapping.class == annotation.annotationType()) {
                RequestMapping request = (RequestMapping) annotation;
                RequestMethod[] requestMethods = request.method();
                requestMethod = requestMethods.length > 0 ? requestMethods[0]:RequestMethod.GET;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                continue;
            }
            if (GetMapping.class == annotation.annotationType() ) {
                GetMapping request = (GetMapping) annotation;
                requestMethod = RequestMethod.GET;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                continue;
            }
            if (PostMapping.class == annotation.annotationType()) {
                PostMapping request = (PostMapping) annotation;
                requestMethod = RequestMethod.POST;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                continue;
            }
            if (PutMapping.class == annotation.annotationType()) {
                PutMapping request = (PutMapping) annotation;
                requestMethod = RequestMethod.PUT;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                continue;
            }
            if (DeleteMapping.class == annotation.annotationType()) {
                DeleteMapping request = (DeleteMapping) annotation;
                requestMethod = RequestMethod.DELETE;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                continue;
            }
            // 中文含义
            if (Desc.class == annotation.annotationType()) {
                Desc descAnno = (Desc) annotation;
                desc = descAnno.value();
            }
            // 中文含义
            if (ApiDesc.class == annotation.annotationType()) {
                ApiDesc descAnno = (ApiDesc) annotation;
                desc = descAnno.value();
            }
        }

        if (uri == null || requestMethod == null ){
            return null;
        }

        if (!uri.startsWith("/")){
            uri = "/" + uri;
        }
        if (prefix != null) {
            uri = prefix + uri;
        }

        // 确定是 rest 接口，提取信息
        RestInfo restInfo = new RestInfo();
        restInfo.setMethod(requestMethod.name());
        restInfo.setUri(uri);
        restInfo.setDesc(desc);

        // 方法名
        String restName = uri.substring(1);
        restName = restName.replace("-", "_");
        restName = restName.replace("/", "_");
        restName = restName.replace("{", "");
        restName = restName.replace("}", "");
        restName = restName.replace("*", "");
        restName = StringUtil.underlineToCamel(restName);
        restInfo.setName(restName);

        // 提取参数信息
        List<RestParam> parameters = extractParameters(method);
        restInfo.setParameters(parameters);

        // 提取返回类型信息
        extractReturnType(method, restInfo);

        return restInfo;
    }

    /**
     * 提取方法参数信息
     *
     * @param method 方法对象
     * @return 参数列表
     */
    private static List<RestParam> extractParameters(Method method) {
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

            for (Annotation annotation : annotations) {
                // 处理 @RequestBody
                if (annotation instanceof RequestBody) {
                    RequestBody requestBody = (RequestBody) annotation;
                    annotationType = "RequestBody";
                    required = requestBody.required();
                    logger.debug("Parameter {} is annotated with @RequestBody, required: {}", paramName, required);
                    break;
                }

                // 处理 @PathVariable
                if (annotation instanceof PathVariable) {
                    PathVariable pathVariable = (PathVariable) annotation;
                    annotationType = "PathVariable";
                    required = pathVariable.required();
                    logger.debug("Parameter {} is annotated with @PathVariable, required: {}", paramName, required);
                    break;
                }

                // 处理 @RequestParam
                if (annotation instanceof RequestParam) {
                    RequestParam requestParam = (RequestParam) annotation;
                    annotationType = "RequestParam";
                    required = requestParam.required();
                    // 默认值
                    String defValue = requestParam.defaultValue();
                    // Spring 的默认值标记，表示没有设置默认值
                    if (defValue != null && !defValue.equals("\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n")) {
                        defaultValue = defValue;
                        logger.debug("Parameter {} is annotated with @RequestParam, required: {}, defaultValue: {}", paramName, required, defaultValue);
                    } else {
                        logger.debug("Parameter {} is annotated with @RequestParam, required: {}", paramName, required);
                    }
                    break;
                }
            }

            // 如果没有找到参数注解，则默认为普通参数
            if (annotationType == null) {
                annotationType = "Parameter";
                required = false;
                logger.debug("Parameter {} has no annotation, treated as plain parameter", paramName);
            }

            restParam.setAnnotationType(annotationType);
            restParam.setRequired(required);
            restParam.setDefaultValue(defaultValue);

            // 提取泛型类型信息
            Type genericType = parameter.getParameterizedType();
            List<String> genericTypes = extractGenericTypes(genericType);
            restParam.setGenericTypes(genericTypes);

            if (CollectionUtils.isNotEmpty(genericTypes)) {
                logger.debug("Parameter {} has generic types: {}", paramName, genericTypes);
            }

            paramList.add(restParam);
        }

        logger.debug("Extracted {} parameters for method: {}", paramList.size(), method.getName());
        return paramList;
    }

    /**
     * 提取泛型类型信息
     *
     * @param type 类型对象
     * @return 泛型类型列表
     */
    private static List<String> extractGenericTypes(Type type) {
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
     * 提取返回类型信息
     *
     * @param method   方法对象
     * @param restInfo REST 信息对象
     */
    private static void extractReturnType(Method method, RestInfo restInfo) {
        // 获取返回类型
        Class<?> returnType = method.getReturnType();

        // 处理 void 返回类型
        if (returnType == void.class || returnType == Void.class) {
            restInfo.setReturnType("void");
            restInfo.setReturnGenericInfo(null);
            return;
        }

        // 设置返回类型
        restInfo.setReturnType(returnType.getName());

        // 获取返回类型的泛型信息
        Type genericReturnType = method.getGenericReturnType();

        // 解析泛型信息
        GenericTypeInfo genericTypeInfo = parseGenericType(genericReturnType);

        // 如果有泛型信息，则序列化为 JSON
        if (genericTypeInfo != null && CollectionUtils.isNotEmpty(genericTypeInfo.getTypeArgs())) {
            restInfo.setReturnGenericInfo(JSONObject.toJSONString(genericTypeInfo));
        } else {
            restInfo.setReturnGenericInfo(null);
        }
    }

    /**
     * 递归解析泛型类型信息
     *
     * @param type 类型对象
     * @return 泛型类型信息
     */
    private static GenericTypeInfo parseGenericType(Type type) {
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
     * 泛型类型信息内部类
     */
    private static class GenericTypeInfo {
        /**
         * 原始类型（完整类名）
         */
        private String rawType;

        /**
         * 类型参数列表
         */
        private List<GenericTypeInfo> typeArgs;

        public String getRawType() {
            return rawType;
        }

        public void setRawType(String rawType) {
            this.rawType = rawType;
        }

        public List<GenericTypeInfo> getTypeArgs() {
            return typeArgs;
        }

        public void setTypeArgs(List<GenericTypeInfo> typeArgs) {
            this.typeArgs = typeArgs;
        }
    }

    private static void appendDesc(List<Class<?>> routerClassList, List<RestInfo> rests) {
        if (CollectionUtils.isEmpty(routerClassList) || CollectionUtils.isEmpty(rests)) {
            return;
        }

        Map<String, List<RestInfo>> restsMap = rests.stream().collect(Collectors.groupingBy(RestInfo::getUri));
        for (Class<?> routerClazz : routerClassList) {
            Field[] fields = routerClazz.getDeclaredFields();
            try {
                Router routerAnno = routerClazz.getAnnotation(Router.class);
                String module = null;
                String prefix = null;
                if (routerAnno != null) {
                    module = routerAnno.module();
                    prefix = routerAnno.prefix();
                    if (StringUtils.isBlank(routerAnno.prefix())) {
                        prefix = "";
                    }
                }


                // 填充 module
                if (module != null) {
                    // 获取 routerClazz 的包名
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

                // 填充 desc
                for (Field field : fields) {
                    Object o = field.get(null);
                    if (o == null) {
                        continue;
                    }
                    Desc desc = field.getAnnotation(Desc.class);
                    ApiDesc apiDesc = field.getAnnotation(ApiDesc.class);

                    String value = null;
                    if (desc != null) {
                        value = desc.value();
                    }
                    if (apiDesc != null) {
                        value = apiDesc.value();
                    }
                    if (StringUtils.isBlank(value)) {
                        continue;
                    }

                    String uri = o.toString();
                    String fullUri = prefix + uri;
                    // 找到 restInfo
                    List<RestInfo> restInfos = restsMap.get(fullUri);
                    if (CollectionUtils.isEmpty(restInfos)) {
                        continue;
                    }
                    for (RestInfo restInfo : restInfos) {
                        restInfo.setDesc(value);
                        restInfo.setWriteFlag(uri.contains("/public/")?1:0);
                    }
                }
            } catch (IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }


}
