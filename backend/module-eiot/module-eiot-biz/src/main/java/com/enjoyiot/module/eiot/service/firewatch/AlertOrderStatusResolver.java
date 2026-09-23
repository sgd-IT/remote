package com.enjoyiot.module.eiot.service.firewatch;

import cn.hutool.core.util.StrUtil;
import com.enjoyiot.module.eiot.dal.dataobject.AlertRecordDO;

/**
 * 告警工单状态解析工具
 */
public final class AlertOrderStatusResolver {

    public static final String ORDER_STATUS_CASE_SQL =
            "(CASE " +
            "WHEN handle_status = 2 THEN 'handling' " +
            "WHEN handle_status = 3 THEN 'handled' " +
            "WHEN details IS NOT NULL AND details LIKE '%关闭工单%' THEN 'closed' " +
            "WHEN details IS NOT NULL AND (details LIKE '%确认真实火警%' OR details LIKE '%标记误报%' OR details LIKE '%手动处理%') THEN 'handled' " +
            "WHEN details IS NOT NULL AND details LIKE '%确认火警%' THEN 'handling' " +
            "WHEN handle_status IN (0, 1) THEN 'pending' " +
            "WHEN read_flg = 1 THEN 'handled' " +
            "ELSE 'pending' END)";

    private AlertOrderStatusResolver() {
    }

    /**
     * 解析告警记录的工单状态
     *
     * <p>优先级：handle_status（权威字段）> details 处置记录 > readFlg 兜底。
     *
     * @param record 告警记录
     * @return pending / handling / handled / closed
     */
    public static String resolveOrderStatus(AlertRecordDO record) {
        if (record == null) {
            return "pending";
        }
        // 1. 优先使用 handle_status（由处置流程显式维护，为权威状态字段）
        Integer handleStatus = record.getHandleStatus();
        if (handleStatus != null) {
            switch (handleStatus) {
                case 2:
                    return "handling"; // 处置中
                case 3:
                    return "handled";  // 已处置
                default:
                    break; // 0=待处置、1=稍后处置 → 继续检查 details（可能被 Web 流程关闭）
            }
        }
        // 2. handle_status 未设置或为 0/1 时，解析 details 末尾的处置记录（Web 处置流程追加 [时间] 处理人 - 动作：备注）
        String details = record.getDetails();
        if (StrUtil.isNotBlank(details)) {
            int lastBracket = details.lastIndexOf('[');
            if (lastBracket >= 0) {
                String lastRecord = details.substring(lastBracket);
                if (lastRecord.contains("关闭工单")) {
                    return "closed";
                }
                if (lastRecord.contains("确认真实火警") || lastRecord.contains("标记误报") || lastRecord.contains("手动处理")) {
                    return "handled";
                }
                if (lastRecord.contains("确认火警")) {
                    return "handling";
                }
                // 有 [ 但不是处置记录（如通知正文），继续按 readFlg 判断
            }
        }
        // 3. 兼容旧数据：未设置 handle_status 且无 details 处置记录时，readFlg=true 等价于已处置
        return Boolean.TRUE.equals(record.getReadFlg()) ? "handled" : "pending";
    }
}
