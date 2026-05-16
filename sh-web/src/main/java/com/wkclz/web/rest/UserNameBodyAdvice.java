package com.wkclz.web.rest;

import com.wkclz.core.base.BaseEntity;
import com.wkclz.core.base.R;
import com.wkclz.core.spi.UserNameProvider;
import com.wkclz.spring.config.SpringContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@RestControllerAdvice
public class UserNameBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final int MAX_DEPTH = 8;

    private static final Map<Class<?>, List<Field>> FIELD_CACHE = new ConcurrentHashMap<>();

    private volatile UserNameProvider cachedProvider;
    private volatile boolean providerChecked = false;

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType,
                                  @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        if (body == null) {
            return null;
        }

        UserNameProvider provider = getUserNameProvider();
        if (provider == null) {
            return body;
        }

        try {
            List<BaseEntity> entities = new ArrayList<>();
            collectBaseEntities(body, entities, 0);

            if (entities.isEmpty()) {
                return body;
            }

            Set<String> userCodes = new HashSet<>();
            for (BaseEntity entity : entities) {
                if (entity.getCreateBy() != null && !entity.getCreateBy().isEmpty()) {
                    userCodes.add(entity.getCreateBy());
                }
                if (entity.getUpdateBy() != null && !entity.getUpdateBy().isEmpty()) {
                    userCodes.add(entity.getUpdateBy());
                }
            }

            if (userCodes.isEmpty()) {
                return body;
            }

            Map<String, String> nameMap = provider.getNamesByUserCodes(userCodes);
            if (nameMap == null || nameMap.isEmpty()) {
                return body;
            }

            for (BaseEntity entity : entities) {
                if (entity.getCreateBy() != null) {
                    String createByName = nameMap.get(entity.getCreateBy());
                    if (createByName != null) {
                        entity.setCreateByName(createByName);
                    }
                }
                if (entity.getUpdateBy() != null) {
                    String updateByName = nameMap.get(entity.getUpdateBy());
                    if (updateByName != null) {
                        entity.setUpdateByName(updateByName);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fill user names: {}", e.getMessage());
        }

        return body;
    }

    private UserNameProvider getUserNameProvider() {
        if (!providerChecked) {
            synchronized (this) {
                if (!providerChecked) {
                    try {
                        Map<String, UserNameProvider> beans = SpringContextHolder.getApplicationContext().getBeansOfType(UserNameProvider.class);
                        if (!beans.isEmpty()) {
                            cachedProvider = beans.values().iterator().next();
                        }
                    } catch (Exception e) {
                        log.debug("UserNameProvider not found, skip user name filling");
                    }
                    providerChecked = true;
                }
            }
        }
        return cachedProvider;
    }

    private void collectBaseEntities(Object obj, List<BaseEntity> result, int depth) {
        if (obj == null || depth > MAX_DEPTH) {
            return;
        }

        if (obj instanceof BaseEntity entity) {
            result.add(entity);
            collectFromFields(entity, result, depth);
            return;
        }

        if (obj instanceof Object[] array) {
            for (Object item : array) {
                collectBaseEntities(item, result, depth + 1);
            }
            return;
        }

        if (obj instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                collectBaseEntities(item, result, depth + 1);
            }
            return;
        }

        if (obj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                collectBaseEntities(entry.getValue(), result, depth + 1);
            }
            return;
        }

        if (obj instanceof R<?> r) {
            collectBaseEntities(r.getData(), result, depth + 1);
            return;
        }

        collectFromFields(obj, result, depth);
    }

    private void collectFromFields(Object obj, List<BaseEntity> result, int depth) {
        if (obj == null || depth > MAX_DEPTH) {
            return;
        }

        Class<?> clazz = obj.getClass();
        String className = clazz.getName();
        if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("jakarta.")) {
            return;
        }

        List<Field> fields = FIELD_CACHE.computeIfAbsent(clazz, UserNameBodyAdvice::getDeclaredFields);
        for (Field field : fields) {
            try {
                Object value = field.get(obj);
                if (value != null) {
                    collectBaseEntities(value, result, depth + 1);
                }
            } catch (IllegalAccessException e) {
                // skip inaccessible fields
            }
        }
    }

    private static List<Field> getDeclaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            String currentName = current.getName();
            if (currentName.startsWith("java.") || currentName.startsWith("javax.") || currentName.startsWith("jakarta.")) {
                break;
            }
            for (Field field : current.getDeclaredFields()) {
                field.setAccessible(true);
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }
}
