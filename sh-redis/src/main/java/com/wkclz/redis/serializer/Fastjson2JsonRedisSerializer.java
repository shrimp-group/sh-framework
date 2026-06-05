package com.wkclz.redis.serializer;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 基于 fastjson2 的 Redis 序列化器
 * <p>
 * 使用 JSONReader.autoTypeFilter 白名单机制替代 SupportAutoType，仅允许白名单内的类通过 AutoType 实例化，
 * 防止恶意 @type 注入导致的远程代码执行漏洞。
 *
 * @author wkclz
 * @date 2024-07-15
 */
public class Fastjson2JsonRedisSerializer<T> implements RedisSerializer<T> {

    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

    /**
     * 默认 AutoType 白名单，允许框架业务类和常用 Java 类型
     */
    private static final String[] DEFAULT_WHITELIST = {
        "com.wkclz.",
        "java.util.",
        "java.lang.",
        "java.time."
    };

    private final Class<T> clazz;
    private final Filter autoTypeFilter;

    public Fastjson2JsonRedisSerializer(Class<T> clazz) {
        this(clazz, new ArrayList<>());
    }

    public Fastjson2JsonRedisSerializer(Class<T> clazz, List<String> extraWhitelist) {
        this.clazz = clazz;
        List<String> allWhitelist = new ArrayList<>(Arrays.asList(DEFAULT_WHITELIST));
        if (extraWhitelist != null) {
            extraWhitelist.stream()
                .filter(s -> s != null && !s.isBlank())
                .forEach(allWhitelist::add);
        }
        this.autoTypeFilter = JSONReader.autoTypeFilter(allWhitelist.toArray(new String[0]));
    }

    @Override
    public byte[] serialize(T t) throws SerializationException {
        if (t == null) {
            return new byte[0];
        }
        try {
            return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
        } catch (Exception e) {
            throw new SerializationException("Could not serialize: " + e.getMessage(), e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            String str = new String(bytes, DEFAULT_CHARSET);
            return JSON.parseObject(str, clazz, autoTypeFilter);
        } catch (Exception e) {
            throw new SerializationException("Could not deserialize: " + e.getMessage(), e);
        }
    }
}
