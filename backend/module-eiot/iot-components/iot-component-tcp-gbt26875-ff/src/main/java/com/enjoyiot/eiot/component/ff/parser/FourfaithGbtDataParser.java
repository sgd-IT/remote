package com.enjoyiot.eiot.component.ff.parser;

import com.enjoyiot.eiot.component.ff.parser.handler.ExtendedHandlers;
import com.enjoyiot.eiot.component.ff.parser.handler.FourfaithPrivateHandlers;
import com.enjoyiot.eiot.component.ff.parser.handler.StandardHandlers;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * GB/T 26875.3-2011 应用数据单元解析器（门面模式）
 * <p>
 * 重构说明：实际解析逻辑已迁移到各 {@link TypeHandler} 实现，
 * 本类作为统一入口，内部委托 {@link TypeHandlerRegistry} 分发。
 * <p>
 * 新增 TLD 类型时：
 * 1. 在 handler 包中实现 TypeHandler
 * 2. 在 {@link #createDefaultRegistry()} 中注册
 * 无需修改本类和现有 Handler。
 */
@Slf4j
public class FourfaithGbtDataParser {

    private static final TypeHandlerRegistry DEFAULT_REGISTRY = createDefaultRegistry();

    /**
     * 创建默认注册表，注册所有内置 Handler
     */
    public static TypeHandlerRegistry createDefaultRegistry() {
        TypeHandlerRegistry registry = new TypeHandlerRegistry();
        // 标准建筑消防设施类型 (TLD 1~8)
        registry.registerAll(
            new StandardHandlers.SystemStatusHandler(),
            new StandardHandlers.ComponentStatusHandler(),
            new StandardHandlers.AnalogValueHandler(),
            new StandardHandlers.OperationInfoHandler(),
            new StandardHandlers.SoftwareVersionHandler(),
            new StandardHandlers.SystemConfigHandler(),
            new StandardHandlers.ComponentConfigHandler(),
            new StandardHandlers.SystemTimeHandler()
        );
        // 用户信息传输装置类型 (TLD 21~28)
        registry.registerAll(
            new StandardHandlers.DeviceStatusHandler(),
            new StandardHandlers.DeviceOperationHandler(),
            new StandardHandlers.DeviceVersionHandler(),
            new StandardHandlers.DeviceConfigHandler(),
            new StandardHandlers.DeviceTimeHandler()
        );
        // 四信私有类型 (TLD 9, 10)
        registry.registerAll(
            new FourfaithPrivateHandlers.PrtHandler(),
            new FourfaithPrivateHandlers.CrtHandler()
        );
        // 拓展版新增类型 (TLD 138, 140, 142, 143, 147, 148)
        registry.registerAll(
            new ExtendedHandlers.EnergyReportHandler(),
            new ExtendedHandlers.MultiChannelStatusHandler(),
            new ExtendedHandlers.DeviceStatusAnalogHandler(),
            new ExtendedHandlers.StatusAnalog32Handler(),
            new ExtendedHandlers.MultiChannelCompTypeHandler(),
            new ExtendedHandlers.MultiChannelDataHandler()
        );
        return registry;
    }

    /**
     * 解析应用数据单元（使用默认注册表）
     */
    public static List<Map<String, Object>> parse(byte[] data) {
        return DEFAULT_REGISTRY.parse(data);
    }

    /**
     * 获取应用数据中的类型标志（第1字节）
     */
    public static int getTypeFlag(byte[] data) {
        return TypeHandlerRegistry.getTypeFlag(data);
    }

    /**
     * 获取默认注册表实例（支持运行时动态注册新 Handler）
     */
    public static TypeHandlerRegistry getRegistry() {
        return DEFAULT_REGISTRY;
    }
}
