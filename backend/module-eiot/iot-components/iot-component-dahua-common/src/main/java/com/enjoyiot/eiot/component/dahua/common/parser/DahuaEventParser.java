package com.enjoyiot.eiot.component.dahua.common.parser;

import com.enjoyiot.eiot.component.dahua.common.constants.DahuaProtocolConstants;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大华协议事件类型解析器（dataType=1）
 */
@Slf4j
public class DahuaEventParser implements DahuaDataTypeParser {

    @Override
    public int getDataType() {
        return DahuaProtocolConstants.DATA_TYPE_EVENT;
    }

    @Override
    public Map<String, Object> parse(byte[] dataContent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("data_type", DahuaProtocolConstants.DATA_TYPE_EVENT);
        result.put("data_typeName", "事件");

        if (dataContent == null || dataContent.length < DahuaProtocolConstants.EVENT_VALUE_LENGTH) {
            log.warn("[解析器] 数据内容不足: length={}", dataContent == null ? 0 : dataContent.length);
            result.put("data_rawHex", encodeHex(dataContent));
            return result;
        }

        ByteBuffer buf = ByteBuffer.wrap(dataContent).order(ByteOrder.LITTLE_ENDIAN);

        // 事件值（2B）
        int rawValue = buf.getShort() & 0xFFFF;
        int subType = rawValue & 0xFF;         // 低字节：子类型
        int alarmType = (rawValue >> 8) & 0xFF; // 高字节：报警类型

        result.put("data_eventValue", rawValue);
        result.put("data_subType", subType);
        result.put("data_subTypeName", getSubTypeName(subType));
        result.put("data_alarmType", alarmType);
        result.put("data_alarmTypeName", getAlarmTypeName(subType, alarmType));

        // 如果有剩余字节，可能是嵌套TLV，暂存为原始hex
        if (buf.remaining() >= 2) {
            byte[] remaining = new byte[buf.remaining()];
            buf.get(remaining);
            // 尝试作为嵌套TLV解析
            int nestedTypeFlag = remaining[0] & 0xFF;
            int nestedCount = remaining[1] & 0xFF;
            result.put("data_nestedTlv_typeFlag", nestedTypeFlag);
            result.put("data_nestedTlv_count", nestedCount);
            result.put("data_nestedTlv_rawHex", encodeHex(remaining));
            log.info("[解析器] 事件含嵌套TLV: typeFlag={}, count={}, remainLen={}",
                    nestedTypeFlag, nestedCount, remaining.length);
        }

        return result;
    }

    // ==================== 事件子类型名称（表 3.2.2-1，0~80） ====================

    private static String getSubTypeName(int subType) {
        switch (subType) {
            case 0: return "预留";
            case 1: return "烟雾报警(开始)";
            case 2: return "烟雾报警(结束)";
            case 3: return "传感器故障(开始)";
            case 4: return "传感器故障(结束)";
            case 5: return "屏蔽(开始)";
            case 6: return "屏蔽(结束)";
            case 7: return "监管(开始)";
            case 8: return "监管(结束)";
            case 9: return "启动";
            case 10: return "停止";
            case 11: return "反馈";
            case 12: return "延时";
            case 13: return "电源故障(开始)";
            case 14: return "电源故障(结束)";
            case 15: return "总线故障(开始)";
            case 16: return "总线故障(结束)";
            case 17: return "手动(开始)";
            case 18: return "手动(结束/自动)";
            case 19: return "配置改变";
            case 20: return "防拆(开始)";
            case 21: return "防拆(结束)";
            case 22: return "气体浓度报警(开始)";
            case 23: return "气体浓度报警(结束)";
            case 24: return "温度报警(开始)";
            case 25: return "温度报警(结束)";
            case 26: return "液压液位报警(开始)";
            case 27: return "液压液位报警(结束)";
            case 28: return "电流报警(开始)";
            case 29: return "电流报警(结束)";
            case 30: return "电压报警(开始)";
            case 31: return "电压报警(结束)";
            case 32: return "功率报警(开始)";
            case 33: return "功率报警(结束)";
            case 34: return "三相不平衡(开始)";
            case 35: return "三相不平衡(结束)";
            case 36: return "电弧/打火报警(开始)";
            case 37: return "电弧/打火报警(结束)";
            case 38: return "失联故障(开始)";
            case 39: return "失联故障(结束)";
            case 40: return "湿度报警(开始)";
            case 41: return "湿度报警(结束)";
            case 42: return "谐波报警(开始)";
            case 43: return "谐波报警(结束)";
            case 44: return "开关量输入事件(开始)";
            case 45: return "开关量输入事件(结束)";
            case 46: return "逆相序报警(开始)";
            case 47: return "逆相序报警(结束)";
            case 48: return "浪涌报警(开始)";
            case 49: return "浪涌报警(结束)";
            case 50: return "短路报警(开始)";
            case 51: return "短路报警(结束)";
            case 52: return "合闸事件(开始)";
            case 53: return "合闸事件(结束)";
            case 54: return "气流故障(开始)";
            case 55: return "气流故障(结束)";
            case 56: return "探测故障(开始)";
            case 57: return "探测故障(结束)";
            case 58: return "设备振动报警(开始)";
            case 59: return "设备振动报警(结束)";
            case 60: return "断电报警(开始)";
            case 61: return "断电报警(结束)";
            case 62: return "空开分闸报警(开始)";
            case 63: return "空开分闸报警(结束)";
            case 64: return "开关量输出联动事件(开始)";
            case 65: return "开关量输出联动事件(结束)";
            case 66: return "反馈结束";
            case 67: return "延时结束";
            case 68: return "手动报警(开始)";
            case 69: return "手动报警(结束)";
            case 70: return "火焰报警(开始)";
            case 71: return "火焰报警(结束)";
            case 72: return "设备故障(开始)";
            case 73: return "设备故障(结束)";
            case 74: return "设备重启";
            case 75: return "报警(开始)";
            case 76: return "报警(结束)";
            case 77: return "故障(开始)";
            case 78: return "故障(结束)";
            case 79: return "消音(开始)";
            case 80: return "消音(结束)";
            default: return "事件类型:" + subType;
        }
    }

    /**
     * 报警类型名称 —— 含义随事件子类型不同而不同
     */
    private static String getAlarmTypeName(int subType, int alarmType) {
        switch (subType) {
            // 烟雾报警 1/2
            case 1: case 2:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "超上限(行动线)";
                    case 4: return "超上限(报警线1)";
                    case 5: return "超上限(报警线2)";
                    default: return "烟雾报警类型:" + alarmType;
                }
            // 传感器故障 3/4
            case 3: case 4:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "短路";
                    case 2: return "断路";
                    default: return "故障类型:" + alarmType;
                }
            // 屏蔽/监管/启动/停止/反馈/延时/总线故障/配置改变/防拆/失联/手动/手动报警/火焰/报警/故障/消音
            // 以及各结束帧 —— 高字节均为预留
            case 5: case 6: case 7: case 8: case 9: case 10: case 11: case 12:
            case 15: case 16: case 19: case 20: case 21:
            case 38: case 39:
            case 17: case 18: case 68: case 69:
            case 70: case 71: case 75: case 76: case 77: case 78: case 79: case 80:
                return alarmType == 0 ? "未区分" : "预留:" + alarmType;
            // 电源故障 13/14 —— 高字节=板位
            case 13: case 14:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "主电";
                    case 2: return "备电";
                    case 3: return "通信板";
                    default: return "板位:" + alarmType;
                }
            // 气体浓度报警 22/23
            case 22: case 23:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "超下限(预警线)";
                    case 4: return "超下限(报警线)";
                    default: return "气体报警类型:" + alarmType;
                }
            // 温度报警 24/25
            case 24: case 25:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "骤变";
                    default: return "温度报警类型:" + alarmType;
                }
            // 液压液位报警 26/27
            case 26: case 27:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "超下限(预警线)";
                    case 4: return "超下限(报警线)";
                    case 5: return "超波动(预警线)";
                    case 6: return "超波动(报警线)";
                    default: return "液位报警类型:" + alarmType;
                }
            // 电流报警 28/29
            case 28: case 29:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "骤变";
                    case 4: return "超下限(预警线)";
                    case 5: return "超下限(报警线)";
                    default: return "电流报警类型:" + alarmType;
                }
            // 电压报警 30/31
            case 30: case 31:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "超下限(预警线)";
                    case 4: return "超下限(报警线)";
                    case 5: return "断相(缺相)";
                    case 6: return "骤变";
                    default: return "电压报警类型:" + alarmType;
                }
            // 功率报警 32/33
            case 32: case 33:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    default: return "功率报警类型:" + alarmType;
                }
            // 三相不平衡 34/35
            case 34: case 35:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    default: return "不平衡类型:" + alarmType;
                }
            // 电弧/打火 36/37、短路 50/51
            case 36: case 37: case 50: case 51:
                return alarmType == 0 ? "未区分" : "报警类型:" + alarmType;
            // 湿度报警 40/41
            case 40: case 41:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "超下限(预警线)";
                    case 4: return "超下限(报警线)";
                    default: return "湿度报警类型:" + alarmType;
                }
            // 谐波报警 42/43
            case 42: case 43:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "超上限(预警线)";
                    case 2: return "超上限(报警线)";
                    case 3: return "超下限(预警线)";
                    case 4: return "超下限(报警线)";
                    default: return "谐波报警类型:" + alarmType;
                }
            // 开关量输入 44/45、开关量输出 64/65
            case 44: case 45: case 64: case 65:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "复位";
                    case 2: return "报警";
                    case 3: return "故障";
                    default: return "开关量类型:" + alarmType;
                }
            // 逆相序 46/47、浪涌 48/49、合闸 52/53、气流 54/55、振动 58/59、断电 60/61、空开 62/63
            case 46: case 47: case 48: case 49: case 52: case 53:
            case 54: case 55: case 58: case 59: case 60: case 61: case 62: case 63:
                return alarmType == 0 ? "未区分" : "预留:" + alarmType;
            // 探测故障 56/57
            case 56: case 57:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "风扇故障";
                    case 2: return "CO传感器故障";
                    case 3: return "探测腔故障";
                    default: return "探测故障类型:" + alarmType;
                }
            // 反馈结束 66、延时结束 67
            case 66: case 67:
                return "同前";
            // 设备故障 72/73 —— 高字节=事件内容
            case 72: case 73:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "直控盘短路";
                    case 2: return "直控盘断路";
                    case 3: return "烟雾传感器故障";
                    case 4: return "温度传感器故障";
                    case 5: return "请求维保";
                    case 6: return "输入1短路故障";
                    case 7: return "输入1断路故障";
                    case 8: return "输出1短路故障";
                    case 9: return "输出1断路故障";
                    case 10: return "探测器电源故障";
                    case 11: return "注册设备掉线";
                    case 12: return "缺纸";
                    case 13: return "输入电源欠压";
                    case 14: return "主电连接故障";
                    case 15: return "备电欠压";
                    case 16: return "备电警示";
                    case 17: return "备电连接故障";
                    case 18: return "从机掉线";
                    case 19: return "打印机掉线";
                    case 20: return "用传掉线";
                    case 21: return "广播主机掉线";
                    case 22: return "应急疏散系统掉线";
                    case 23: return "防火门监视系统掉线";
                    case 24: return "消防电源监视系统掉线";
                    case 25: return "电气火灾监视系统掉线";
                    case 26: return "余压监视系统掉线";
                    case 27: return "CRT掉线";
                    case 28: return "分区启动断路";
                    case 29: return "分区反馈断路";
                    case 30: return "总线短路";
                    case 31: return "主机按键故障";
                    case 32: return "新增设备上线";
                    case 33: return "回路板掉线";
                    case 34: return "直控盘掉线";
                    case 35: return "总线盘掉线";
                    case 36: return "灯键盘掉线";
                    case 37: return "火警输出短路";
                    case 38: return "火警输出开路";
                    case 39: return "故障输出短路";
                    case 40: return "故障输出开路";
                    case 41: return "24V输出短路";
                    case 42: return "24V输出开路";
                    case 43: return "分区启动短路";
                    case 44: return "分区反馈短路";
                    case 45: return "批量设备新增";
                    case 46: return "编码重号";
                    case 47: return "重复上报";
                    case 48: return "替换设备";
                    default: return "设备故障内容:" + alarmType;
                }
            // 设备重启 74 —— 高字节=设备类型
            case 74:
                switch (alarmType) {
                    case 0: return "未区分";
                    case 1: return "主机";
                    case 2: return "节点";
                    case 3: return "节点+主机";
                    default: return "设备类型:" + alarmType;
                }
            default:
                return "报警类型:" + alarmType;
        }
    }

    private static String encodeHex(byte[] data) {
        if (data == null || data.length == 0) return "";
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }
}
