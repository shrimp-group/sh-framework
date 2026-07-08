package com.wkclz.web.helper;

import com.alibaba.fastjson2.JSONObject;
import com.wkclz.tool.utils.ClassUtil;
import com.wkclz.tool.utils.StringUtil;
import com.wkclz.web.bean.RestInfo;
import com.wkclz.web.bean.RestParam;
import io.swagger.v3.oas.annotations.Operation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * REST 接口扫描工具类
 * 用于扫描项目的所有 RESTful 接口，支持 Swagger 注解解析和结构扫描
 *
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
        List<Class<?>> routerClassList = classes.stream().filter(clazz -> clazz.isAnnotationPresent(com.wkclz.core.annotation.Router.class)).toList();
        for (Class<?> clazz : restClassList) {
            // 提取类级别 @Tag 注解
            String classTag = RestAnnotationHelper.extractClassTag(clazz);
            logger.debug("Class {} tag: {}", clazz.getName(), classTag);

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
                RestInfo rest = getRest(method, prefix, classTag);
                if (rest != null) {
                    rest.setClazz(clazz);
                    rests.add(rest);
                }
            }
        }

        RestAnnotationHelper.appendDesc(routerClassList, rests);

        if (StringUtils.isNotBlank(filter)) {
            rests = rests.stream().filter(t -> t.getUri().contains(filter)).toList();
        }
        if (StringUtils.isNotBlank(appCode)) {
            rests.forEach(t -> t.setAppCode(appCode));
        }
        return rests;
    }

    private static RestInfo getRest(Method method, String prefix, String classTag) {
        if (method == null) {
            return null;
        }
        Annotation[] annotations = method.getAnnotations();
        String uri = null;
        String desc = null;
        RequestMethod requestMethod = null;
        String[] consumes = null;
        String[] produces = null;

        // Swagger @Operation 信息
        String operationSummary = null;
        String operationDescription = null;
        Boolean operationDeprecated = null;

        for (Annotation annotation : annotations) {
            if (RequestMapping.class == annotation.annotationType()) {
                RequestMapping request = (RequestMapping) annotation;
                RequestMethod[] requestMethods = request.method();
                requestMethod = requestMethods.length > 0 ? requestMethods[0]:RequestMethod.GET;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                // 提取 consumes 和 produces
                consumes = request.consumes();
                produces = request.produces();
                continue;
            }
            if (GetMapping.class == annotation.annotationType() ) {
                GetMapping request = (GetMapping) annotation;
                requestMethod = RequestMethod.GET;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                consumes = request.consumes();
                produces = request.produces();
                continue;
            }
            if (PostMapping.class == annotation.annotationType()) {
                PostMapping request = (PostMapping) annotation;
                requestMethod = RequestMethod.POST;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                consumes = request.consumes();
                produces = request.produces();
                continue;
            }
            if (PutMapping.class == annotation.annotationType()) {
                PutMapping request = (PutMapping) annotation;
                requestMethod = RequestMethod.PUT;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                consumes = request.consumes();
                produces = request.produces();
                continue;
            }
            if (DeleteMapping.class == annotation.annotationType()) {
                DeleteMapping request = (DeleteMapping) annotation;
                requestMethod = RequestMethod.DELETE;
                String[] values = request.value();
                uri = values.length == 0 ? null : values[0];
                consumes = request.consumes();
                produces = request.produces();
                continue;
            }
            // Swagger @Operation
            if (annotation instanceof Operation) {
                Operation operation = (Operation) annotation;
                operationSummary = operation.summary();
                operationDescription = operation.description();
                operationDeprecated = operation.deprecated();
                logger.debug("Method {} has @Operation: summary={}, description={}, deprecated={}",
                    method.getName(), operationSummary, operationDescription, operationDeprecated);
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

        // desc 优先取 @Operation.summary，回退到 description
        if (StringUtils.isBlank(desc) && StringUtils.isNotBlank(operationSummary)) {
            desc = operationSummary;
        } else if (StringUtils.isBlank(desc) && StringUtils.isNotBlank(operationDescription)) {
            desc = operationDescription;
        }

        // 确定是 rest 接口，提取信息
        RestInfo restInfo = new RestInfo();
        restInfo.setMethod(requestMethod.name());
        restInfo.setUri(uri);
        restInfo.setDesc(desc);
        restInfo.setTag(classTag);
        restInfo.setOperationSummary(operationSummary);
        restInfo.setOperationDescription(operationDescription);
        restInfo.setDeprecated(operationDeprecated);
        restInfo.setConsumes(consumes);
        restInfo.setProduces(produces);
        restInfo.setWriteFlag(uri.contains("/public/") ? 1 : 0);

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
        List<RestParam> parameters = RestAnnotationHelper.extractParameters(method);
        restInfo.setParameters(parameters);

        // 提取返回类型信息
        RestAnnotationHelper.extractReturnType(method, restInfo);

        return restInfo;
    }
}
