package com.enjoyiot.eiot.ruleengine.action.alert;

import cn.hutool.core.date.DateUtil;
import com.enjoyiot.eiot.common.thing.ThingModelMessage;
import com.enjoyiot.eiot.ruleengine.action.ScriptService;
import com.enjoyiot.eiot.message.service.MessageService;
import com.enjoyiot.framework.tenant.core.context.TenantContextHolder;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import com.enjoyiot.module.eiot.api.device.DeviceApi;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;
import java.util.Map;

/**
 * @author sjg
 */
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Data
public class AlertService extends ScriptService {

    /** 模板中事件时间变量的两种命名（历史模板并存：event_time / eventTime） */
    private static final String[] EVENT_TIME_KEYS = {"event_time", "eventTime"};

    private Message message;

    private MessageService messageService;

    /**
     * 告警解除脚本，未配置则复用触发脚本
     */
    private String recoverScript;

    private final ScriptService recoverExecutor = new ScriptService();

    public void initDeviceApi(DeviceApi deviceApi) {
        super.setDeviceApi(deviceApi);
        recoverExecutor.setDeviceApi(deviceApi);
    }

    public void setRecoverScript(String recoverScript) {
        this.recoverScript = recoverScript;
        if (StringUtils.isNotBlank(recoverScript)) {
            recoverExecutor.setScript(recoverScript);
        }
    }

    @SneakyThrows
    public String execute(ThingModelMessage msg) {
        return executeInternal(msg, false);
    }

    public String executeRecover(ThingModelMessage msg) {
        return executeInternal(msg, true);
    }

    @SneakyThrows
    private String executeInternal(ThingModelMessage msg, boolean recover) {
        // 执行转换脚本
        Map<String, Object> result = executeScript(msg, recover);
        
        // 验证脚本执行结果
        if (!validateScriptResult(result, recover)) {
            return "execScript result is null";
        }
        
        // 为消息增加状态标记，便于模板取值
        enrichMessageResult(result, recover);

        // 事件时间兜底：设备上报未携带时用消息时间补齐，避免模板占位符裸露入库
        enrichEventTime(result, msg);
        
        message.setDeviceId(msg.getDeviceId());
        
        message.setProductKey(msg.getProductKey());
        
        // 发送告警消息
        sendAlertMessage(result, msg);
        
        return getResultMessage(recover);
    }

    /**
     * 执行脚本：根据是否为恢复操作选择对应的脚本执行器
     * 
     * @param msg 物模型消息
     * @param recover 是否为恢复操作
     * @return 脚本执行结果
     */
    private Map<String, Object> executeScript(ThingModelMessage msg, boolean recover) {
        if (recover && StringUtils.isNotBlank(recoverScript)) {
            // 使用恢复脚本执行器
            return recoverExecutor.execScript(new TypeReference<Map<String, Object>>() {}, msg);
        } else {
            // 使用触发脚本执行器
            return execScript(new TypeReference<Map<String, Object>>() {}, msg);
        }
    }

    /**
     * 丰富消息结果：添加状态标记等信息
     * 
     * @param result 脚本执行结果
     * @param recover 是否为恢复操作
     */
    private void enrichMessageResult(Map<String, Object> result, boolean recover) {
        result.putIfAbsent("alertState", recover ? "recover" : "alert");
    }

    /**
     * 事件时间兜底：模板引用的时间变量（模板 15 用 event_time，模板 7/11/13 用 eventTime）
     * 在设备上报未携带或为空时，依次回退 设备事件时间(occurred) → 消息上报时间(time) → 服务端当前时间。
     * 格式与设备上报保持一致（yyyy-MM-dd HH:mm:ss），避免 ${xxx} 占位符裸露入库。
     *
     * @param result 脚本执行结果（即模板参数 param）
     * @param msg    物模型消息
     */
    private void enrichEventTime(Map<String, Object> result, ThingModelMessage msg) {
        String formatted = null;
        for (String key : EVENT_TIME_KEYS) {
            if (!isBlankValue(result.get(key))) {
                continue;
            }
            if (formatted == null) {
                formatted = formatEventTime(msg);
            }
            result.put(key, formatted);
        }
    }

    private String formatEventTime(ThingModelMessage msg) {
        Long millis = null;
        if (msg != null) {
            millis = msg.getOccurred() != null ? msg.getOccurred() : msg.getTime();
        }
        if (millis == null) {
            millis = System.currentTimeMillis();
        }
        return DateUtil.formatDateTime(new Date(millis));
    }

    private static boolean isBlankValue(Object value) {
        return value == null || (value instanceof CharSequence && StringUtils.isBlank((CharSequence) value));
    }

    /**
     * 发送告警消息
     *
     * @param result 消息参数
     */
    private void sendAlertMessage(Map<String, Object> result, ThingModelMessage msg) {
        message.setParam(result);
        // tenantId 已在 AlertApiImpl.getNotifyMessage() 中从 AlertConfig 带入
        // 规则引擎在 TenantUtils.executeIgnore 下执行，TenantContextHolder 可能为空，不从此处取
        // 填充设备信息，用于 SmsEventListener 通过 tenantId 查找企业联系人
        if (msg != null) {
            message.setDn(msg.getDn());
            message.setProductKey(msg.getProductKey());
        }
        messageService.sendMessage(message);
    }

    /**
     * 验证脚本执行结果
     * 
     * @param result 脚本执行结果
     * @param recover 是否为恢复操作
     * @return 验证是否通过
     */
    private boolean validateScriptResult(Map<String, Object> result, boolean recover) {
        if (result == null) {
            log.warn("execScript result is null, recover={}", recover);
            return false;
        }
        return true;
    }

    /**
     * 获取执行结果消息
     * 
     * @param recover 是否为恢复操作
     * @return 结果消息
     */
    private String getResultMessage(boolean recover) {
        return recover ? "recover_ok" : "ok";
    }
}
