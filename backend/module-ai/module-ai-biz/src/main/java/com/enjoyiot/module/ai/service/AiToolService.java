package com.enjoyiot.module.ai.service;

import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import com.enjoyiot.module.eiot.service.firewatch.FireDashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 工具服务 - 定义可被大模型调用的系统工具
 */
@Service
@Slf4j
public class AiToolService {

    @Resource
    private FireDashboardService fireDashboardService;

    @Resource
    private ObjectMapper objectMapper;

    public static final String TOOL_CALL_PREFIX = "[TOOL_CALL]";
    public static final String TOOL_CALL_SUFFIX = "[/TOOL_CALL]";

    public static final LinkedHashMap<String, String> TOOL_DEFINITIONS = new LinkedHashMap<>();

    static {
        TOOL_DEFINITIONS.put("get_device_status",
                "获取设备状态统计。返回：设备总数、在线数、离线数、正常数、告警数、故障数、未激活数、在线率、告警率、故障率。适用场景：用户问设备在线/离线数量、设备健康状态、在线率等。");
        TOOL_DEFINITIONS.put("get_today_alarm",
                "获取今日警情统计。返回：今日告警总数、已处置数、待处理数、误报数、处置率、平均响应时间、火警数、故障告警数。适用场景：用户问今日告警/火警/故障数量、处置率、待处理情况等。");
        TOOL_DEFINITIONS.put("get_realtime_alarms",
                "获取实时告警列表。参数：limit(数量限制,默认10)。适用场景：用户问当前有哪些告警、最近的报警、实时警情等。");
        TOOL_DEFINITIONS.put("get_alarm_trend",
                "获取告警趋势数据。参数：days(天数,默认7)。适用场景：用户问告警趋势、报警趋势分析、最近N天报警情况等。");
        TOOL_DEFINITIONS.put("get_device_category",
                "获取设备类型分布统计。返回各类设施的数量。适用场景：用户问设备分类、有哪些类型的设备、各类型设备数量等。");
        TOOL_DEFINITIONS.put("get_risk_distribution",
                "获取风险单位分布。返回各级风险等级的单位数量。适用场景：用户问风险分布、高风险单位、风险等级等。");
        TOOL_DEFINITIONS.put("get_dashboard_overview",
                "获取监控大屏综合概览数据，包含设备状态、今日警情、风险分布、告警趋势、设备分类、巡检摘要、值班列表等全部统计。适用场景：用户要求全面的系统状态汇报、综合分析、整体概况等。");
    }

    /**
     * 执行工具调用
     */
    public String executeTool(String toolName, String arguments) {
        try {
            Object result = switch (toolName) {
                case "get_device_status" -> executeGetDeviceStatus();
                case "get_today_alarm" -> executeGetTodayAlarm();
                case "get_realtime_alarms" -> executeGetRealtimeAlarms(arguments);
                case "get_alarm_trend" -> executeGetAlarmTrend(arguments);
                case "get_device_category" -> executeGetDeviceCategory();
                case "get_risk_distribution" -> executeGetRiskDistribution();
                case "get_dashboard_overview" -> executeGetDashboardOverview();
                default -> {
                    log.warn("[executeTool] unknown tool: {}", toolName);
                    yield Map.of("error", "unknown tool: " + toolName);
                }
            };
            return objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            log.error("[executeTool] error: toolName={}", toolName, e);
            try {
                return objectMapper.writeValueAsString(Map.of("error", "tool execution failed: " + e.getMessage()));
            } catch (Exception ex) {
                return "{\"error\":\"tool execution failed\"}";
            }
        }
    }

    /**
     * 生成工具描述的 system prompt 片段
     */
    public static String buildToolPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 你可以调用以下工具来获取系统实时数据：\n\n");
        sb.append("当用户的问题需要系统实时数据时，你必须调用对应的工具获取数据后再回答，不要凭空编造数据。\n");
        sb.append("调用格式：在回复中插入以下标记：\n");
        sb.append(TOOL_CALL_PREFIX).append("{\"name\":\"工具名\",\"arguments\":{参数}}").append(TOOL_CALL_SUFFIX).append("\n\n");
        sb.append("你可以一次调用多个工具，每个工具调用单独一行。调用完工具后，根据工具返回的数据继续回答用户问题。\n");
        sb.append("如果工具返回的数据已足够回答用户问题，请直接基于数据给出分析和建议，不要再调用工具。\n\n");
        sb.append("### 可用工具列表：\n\n");
        for (Map.Entry<String, String> entry : TOOL_DEFINITIONS.entrySet()) {
            sb.append("- **").append(entry.getKey()).append("**: ").append(entry.getValue()).append("\n\n");
        }
        return sb.toString();
    }

    // ==================== tool implementations ====================

    private DeviceStatusStatisticsRespVO executeGetDeviceStatus() {
        return fireDashboardService.getDeviceStatusStatistics();
    }

    private TodayAlarmStatisticsRespVO executeGetTodayAlarm() {
        return fireDashboardService.getTodayAlarmStatistics();
    }

    private Object executeGetRealtimeAlarms(String arguments) {
        int limit = 10;
        try {
            if (arguments != null && !arguments.isBlank()) {
                Map<?, ?> map = objectMapper.readValue(arguments, Map.class);
                if (map.containsKey("limit")) {
                    limit = Integer.parseInt(map.get("limit").toString());
                }
            }
        } catch (Exception ignored) {}
        return fireDashboardService.getRealtimeAlarms(limit);
    }

    private Object executeGetAlarmTrend(String arguments) {
        int days = 7;
        try {
            if (arguments != null && !arguments.isBlank()) {
                Map<?, ?> map = objectMapper.readValue(arguments, Map.class);
                if (map.containsKey("days")) {
                    days = Integer.parseInt(map.get("days").toString());
                }
            }
        } catch (Exception ignored) {}
        return fireDashboardService.getAlarmTrend(days);
    }

    private Object executeGetDeviceCategory() {
        return fireDashboardService.getDeviceCategoryDistribution();
    }

    private Object executeGetRiskDistribution() {
        return fireDashboardService.getRiskDistribution();
    }

    private Object executeGetDashboardOverview() {
        return fireDashboardService.getDashboardStatistics();
    }
}
