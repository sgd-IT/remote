package com.enjoyiot.eiot.component.ff.parser;

import com.enjoyiot.eiot.component.ff.parser.handler.DahuaUniversalDataHandler;
import com.enjoyiot.eiot.component.ff.parser.handler.ExtendedHandlers;
import com.enjoyiot.eiot.component.ff.parser.handler.PrivateHandlers;
import com.enjoyiot.eiot.component.ff.parser.handler.StandardHandlers;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * GB/T 26875.3-2011 应用数据单元解析器（门面模式）
 */
@Slf4j
public class GbtDataParser {

    // ponytail: 懒加载，首次设备上报时才初始化 Fourfaith handler 注册表
    private static volatile TypeHandlerRegistry defaultRegistry;

    private static TypeHandlerRegistry getOrCreateRegistry() {
        TypeHandlerRegistry r = defaultRegistry;
        if (r == null) {
            synchronized (GbtDataParser.class) {
                r = defaultRegistry;
                if (r == null) {
                    r = createDefaultRegistry();
                    defaultRegistry = r;
                }
            }
        }
        return r;
    }

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
            new PrivateHandlers.PrtHandler(),
            new PrivateHandlers.CrtHandler()
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
        // 大华协议 0x87 通用数据帧（嵌套TLV，可替代所有上报帧）
        registry.register(new DahuaUniversalDataHandler());
        return registry;
    }

    /**
     * 解析应用数据单元（使用默认注册表）
     */
    public static List<Map<String, Object>> parse(byte[] data) {
        return getOrCreateRegistry().parse(data);
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
        return getOrCreateRegistry();
    }
}
