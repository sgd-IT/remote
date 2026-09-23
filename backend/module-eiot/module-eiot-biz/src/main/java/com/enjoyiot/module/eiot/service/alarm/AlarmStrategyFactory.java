package com.enjoyiot.module.eiot.service.alarm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警策略工厂（工厂模式）
 * <p>
 * 负责管理和路由 {@link AlarmProcessingStrategy} 实现类。通过 Spring 自动注入
 * 所有策略实现，根据 {@link AlarmSourceType} 选择对应的处理策略。
 * <p>
 * 设计要点：
 * <ul>
 *   <li>Spring {@code @Component} 自动发现所有 {@code AlarmProcessingStrategy} Bean</li>
 *   <li>首次访问时构建 type → strategy 缓存，后续 O(1) 查找</li>
 *   <li>未找到匹配策略时返回空列表并记录告警日志，不会抛异常</li>
 * </ul>
 *
 * @author EnjoyIot
 */
@Slf4j
@Component
public class AlarmStrategyFactory {

    /**
     * 所有已注册的策略实现（Spring 自动注入）
     */
    private final List<AlarmProcessingStrategy> strategies;

    /**
     * type → strategy 懒加载缓存（线程安全）
     */
    private final Map<AlarmSourceType, AlarmProcessingStrategy> strategyCache = new ConcurrentHashMap<>();

    /**
     * 构造器注入（Spring 自动收集所有 AlarmProcessingStrategy Bean）
     *
     * @param strategies 所有策略实现列表
     */
    public AlarmStrategyFactory(List<AlarmProcessingStrategy> strategies) {
        this.strategies = strategies != null ? strategies : Collections.emptyList();
    }

    /**
     * 根据来源类型获取对应的告警处理策略
     *
     * @param sourceType 告警来源类型
     * @return 匹配的策略实现，不存在则返回 {@code null}
     */
    public AlarmProcessingStrategy getStrategy(AlarmSourceType sourceType) {
        if (sourceType == null) {
            log.warn("[告警工厂] sourceType 为 null，无法获取策略");
            return null;
        }

        // 懒加载：首次访问时构建缓存
        if (strategyCache.isEmpty() && !strategies.isEmpty()) {
            buildStrategyCache();
        }

        return strategyCache.get(sourceType);
    }

    /**
     * 使用指定策略解析告警上下文，返回标准化告警消息列表
     * <p>
     * 这是提供给调用方的一站式入口：查找策略 + 执行解析 + 异常保护。
     *
     * @param sourceType 告警来源类型
     * @param context    原始告警上下文
     * @return 解析后的告警消息列表（异常或无策略时返回空列表）
     */
    public List<AlarmMessage> parse(AlarmSourceType sourceType,
                                     AlarmProcessingStrategy.AlarmParseContext context) {
        AlarmProcessingStrategy strategy = getStrategy(sourceType);
        if (strategy == null) {
            log.warn("[告警工厂] 未找到匹配的策略, sourceType={}", sourceType);
            return Collections.emptyList();
        }

        try {
            List<AlarmMessage> messages = strategy.parse(context);
            log.info("[告警工厂] 策略解析完成, sourceType={}, strategy={}, 告警数={}",
                    sourceType, strategy.getClass().getSimpleName(), messages.size());
            return messages != null ? messages : Collections.emptyList();
        } catch (Exception e) {
            log.error("[告警工厂] 策略解析异常, sourceType={}, strategy={}",
                    sourceType, strategy.getClass().getSimpleName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取所有已注册的策略
     *
     * @return 不可变列表
     */
    public List<AlarmProcessingStrategy> getAllStrategies() {
        return Collections.unmodifiableList(strategies);
    }

    /**
     * 获取已注册的来源类型集合（用于调试/监控）
     *
     * @return 支持的类型集合
     */
    public Set<AlarmSourceType> getSupportedSourceTypes() {
        if (strategyCache.isEmpty() && !strategies.isEmpty()) {
            buildStrategyCache();
        }
        return Collections.unmodifiableSet(strategyCache.keySet());
    }

    // ==================== 私有方法 ====================

    /**
     * 构建策略缓存
     * <p>
     * 遍历所有策略实现，以 {@link AlarmProcessingStrategy#supportedSourceType()}
     * 为 key 建立映射。重复注册同一类型时记录告警。
     */
    private void buildStrategyCache() {
        for (AlarmProcessingStrategy strategy : strategies) {
            AlarmSourceType type = strategy.supportedSourceType();
            if (type == null) {
                log.warn("[告警工厂] 策略 {} 返回 null sourceType，跳过注册",
                        strategy.getClass().getSimpleName());
                continue;
            }
            AlarmProcessingStrategy existing = strategyCache.put(type, strategy);
            if (existing != null) {
                log.warn("[告警工厂] sourceType={} 已有策略 {}，被 {} 覆盖",
                        type, existing.getClass().getSimpleName(),
                        strategy.getClass().getSimpleName());
            } else {
                log.info("[告警工厂] 注册策略: type={}, strategy={}",
                        type, strategy.getClass().getSimpleName());
            }
        }
    }
}
