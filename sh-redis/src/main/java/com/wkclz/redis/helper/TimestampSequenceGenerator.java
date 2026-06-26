package com.wkclz.redis.helper;

import com.wkclz.core.exception.SystemException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 时间戳序列唯一 ID 生成器
 * 生成 yyMMddHHmmssxxxx 格式的唯一 ID
 * 其中 xxxx 为基于 Redis 的趋势递增序列，步长自适应随机，每秒内趋势递增
 * 序列接近耗尽时主动等待下一秒，避免阻塞
 *
 * @author wkclz
 * @date 2026-06-26
 */
@Slf4j
@Component
public class TimestampSequenceGenerator {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // Redis key 前缀
    private static final String TIMESTAMP_SEQUENCE_KEY_PREFIX = "id:ts:seq:";

    // 序列号最大值
    private static final int MAX_SEQUENCE = 9999;

    // 默认每秒预期生成数
    private static final int DEFAULT_EXPECTED_PER_SECOND = 10;

    // 时间戳格式
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    // 序列号格式（4位补零）
    private static final String SEQUENCE_FORMAT = "%04d";

    // 每个 businessType 的状态
    private final ConcurrentHashMap<String, SequenceState> stateMap = new ConcurrentHashMap<>();

    /**
     * 生成时间戳序列唯一 ID（默认 businessType）
     *
     * @return yyMMddHHmmssxxxx 格式的 16 位字符串
     */
    public String generate() {
        return generate("default");
    }

    /**
     * 生成时间戳序列唯一 ID
     *
     * @param businessType 业务类型，不同业务类型序列独立
     * @return yyMMddHHmmssxxxx 格式的 16 位字符串
     */
    public String generate(String businessType) {
        if (businessType == null || businessType.isEmpty()) {
            businessType = "default";
        }

        long currentEpochSecond = getCurrentEpochSecond();
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 获取或创建该 businessType 的状态
        SequenceState state = stateMap.computeIfAbsent(businessType, k -> new SequenceState());

        // 跨秒检测：更新 expectedPerSecond
        if (currentEpochSecond != state.currentSecond) {
            synchronized (state) {
                if (currentEpochSecond != state.currentSecond) {
                    int lastCount = (int) state.currentSecondCount.get();
                    if (lastCount > 0) {
                        state.expectedPerSecond = lastCount;
                        log.debug("TimestampSequenceGenerator businessType={}, expectedPerSecond updated to {}", businessType, state.expectedPerSecond);
                    }
                    state.currentSecond = currentEpochSecond;
                    state.currentSecondCount.set(0);
                    state.localSequence.set(0);
                    log.debug("TimestampSequenceGenerator businessType={}, second changed to {}", businessType, currentEpochSecond);
                }
            }
        }

        // 计算自适应步长
        int maxStep = Math.max(1, 5000 / state.expectedPerSecond);
        int step = ThreadLocalRandom.current().nextInt(1, maxStep + 1);

        // 计算主动等待阈值
        int threshold = MAX_SEQUENCE - maxStep;

        // 直接从 Redis INCRBY 获取序列号
        String key = TIMESTAMP_SEQUENCE_KEY_PREFIX + businessType + ":" + currentEpochSecond;
        long seq;
        try {
            Long redisSeq = stringRedisTemplate.opsForValue().increment(key, step);
            if (redisSeq == null) {
                redisSeq = (long) step;
            }
            seq = redisSeq;
            // 首次创建 key 时设置过期时间（seq == step 说明 key 之前不存在）
            if (seq <= step) {
                stringRedisTemplate.expire(key, 5, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("TimestampSequenceGenerator Redis increment failed, using local fallback for businessType={}", businessType, e);
            // Redis 不可用，使用本地降级序列
            seq = state.localSequence.addAndGet(step);
        }

        // 检查是否超过主动等待阈值（防阻塞：剩余空间不足一个 maxStep 时主动切换）
        if (seq > threshold) {
            log.info("TimestampSequenceGenerator businessType={}, sequence {} reached threshold {}, proactively waiting for next second", businessType, seq, threshold);
            waitForNextSecond(state.currentSecond);
            return generate(businessType);
        }

        // 计数
        state.currentSecondCount.incrementAndGet();

        // 格式化返回
        String result = timestamp + String.format(SEQUENCE_FORMAT, seq);
        log.debug("TimestampSequenceGenerator businessType={}, seq={}, step={}, result={}", businessType, seq, step, result);
        return result;
    }

    /**
     * 等待下一秒
     *
     * @param currentSecond 当前秒
     */
    private void waitForNextSecond(long currentSecond) {
        long now = getCurrentEpochSecond();
        while (now <= currentSecond) {
            long sleepMs = (currentSecond - now + 1) * 1000 - System.currentTimeMillis() % 1000;
            if (sleepMs > 0) {
                try {
                    Thread.sleep(sleepMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw SystemException.of("Thread interrupted while waiting for next second");
                }
            }
            now = getCurrentEpochSecond();
        }
    }

    /**
     * 获取当前 epoch 秒
     */
    private long getCurrentEpochSecond() {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * 每个 businessType 的序列状态
     */
    private static class SequenceState {
        // 当前秒（epoch second）
        volatile long currentSecond = -1;

        // 当前秒内已生成的 ID 数量
        final AtomicLong currentSecondCount = new AtomicLong(0);

        // 本地降级序列（Redis 不可用时使用）
        final AtomicLong localSequence = new AtomicLong(0);

        // 上一秒实际生成的 ID 数量（用于动态计算 expectedPerSecond）
        volatile int expectedPerSecond = DEFAULT_EXPECTED_PER_SECOND;
    }
}
