package com.enjoyiot.module.eiot.api.alert.sms;

import cn.hutool.http.HttpUtil;
import cn.hutool.core.util.StrUtil;
import com.enjoyiot.module.eiot.api.alert.dto.SmsConfig;
import com.enjoyiot.module.eiot.api.alert.ChannelSmsStrategy;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 东蓝信息短信平台客户端
 * 
 * 接口地址：http://sdk.gzdlinfo.cn/dlcpInterface/sdk/sendSms
 * 
 * @author EnjoyIot
 */
@Slf4j
@Component("gzdlSmsStrategy")
@ConditionalOnProperty(name = "config.alert.sms-provider", havingValue = "gzdl")
public class ChannelSmsGzdlStrategy implements ChannelSmsStrategy {

    /**
     * 短信发送接口URL
     */
    private static final String SMS_SEND_URL = "http://sdk.gzdlinfo.cn/dlcpInterface/sdk/sendSms";

    /**
     * 默认 cdkey（可在配置中覆盖）
     */
    private static final String DEFAULT_CDKEY = "**";

    /**
     * 默认 password（可在配置中覆盖）
     */
    private static final String DEFAULT_PASSWORD = "**";

    @Override
    public void sendSms(Map<String, Object> templateParam, String templateId, SmsConfig smsConfig) {
        try {
            // 获取配置参数
            String cdkey = StrUtil.isNotBlank(smsConfig.getAccessKeyId()) 
                    ? smsConfig.getAccessKeyId() : DEFAULT_CDKEY;
            String password = StrUtil.isNotBlank(smsConfig.getAccessKeySecret()) 
                    ? smsConfig.getAccessKeySecret() : DEFAULT_PASSWORD;
            String phone = smsConfig.getPhoneNumbers();

            if (StrUtil.isBlank(phone)) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.SMS_SEND_FAILED, "手机号不能为空");
            }

            // 构建短信内容
            String message = buildSmsContent(templateParam, templateId);

            // URL编码
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());

            // 构建请求URL
            String url = String.format("%s?cdkey=%s&password=%s&phone=%s&message=%s&addserial=&seqid=",
                    SMS_SEND_URL, cdkey, password, phone, encodedMessage);

            log.info("[GZDL短信平台] 发送短信，手机号: {}, 内容: {}", phone, message);

            // 发送请求
            String response = HttpUtil.get(url);

            log.info("[GZDL短信平台] 响应结果: {}", response);

            // 解析响应结果（假设返回 "success" 或包含错误信息）
            if (!isSuccess(response)) {
                log.error("[GZDL短信平台] 短信发送失败: {}", response);
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.SMS_SEND_FAILED, 
                        "短信发送失败: " + response);
            }

            log.info("[GZDL短信平台] 短信发送成功");
        } catch (Exception e) {
            log.error("[GZDL短信平台] 短信发送异常", e);
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.SMS_SEND_FAILED, 
                    "短信发送异常: " + e.getMessage());
        }
    }

    @Override
    public String createSmsTemplate(String templateContent, Long templateId, SmsConfig smsConfig) {
        // 该第三方平台不支持动态创建模板
        log.warn("[GZDL短信平台] 该平台不支持创建模板功能");
        return null;
    }

    @Override
    public String updateSmsTemplate(String templateContent, String templateCode, SmsConfig smsConfig) {
        // 该第三方平台不支持动态更新模板
        log.warn("[GZDL短信平台] 该平台不支持更新模板功能");
        return null;
    }

    @Override
    public Integer querySmsTemplateStatus(SmsConfig smsConfig, String templateCode) {
        // 该第三方平台不支持查询模板状态
        log.warn("[GZDL短信平台] 该平台不支持查询模板状态功能");
        return null;
    }

    @Override
    public void deleteSmsTemplate(String templateCode, SmsConfig smsConfig) {
        // 该第三方平台不支持删除模板
        log.warn("[GZDL短信平台] 该平台不支持删除模板功能");
    }

    /**
     * 构建短信内容
     * 
     * @param templateParam 模板参数
     * @param templateId 模板ID（短信内容模板）
     * @return 最终的短信内容
     */
    private String buildSmsContent(Map<String, Object> templateParam, String templateId) {
        String content = templateId;
        
        if (templateParam != null && !templateParam.isEmpty()) {
            // 替换模板中的占位符，支持 ${key} 和 {key} 两种格式
            for (Map.Entry<String, Object> entry : templateParam.entrySet()) {
                String key = entry.getKey();
                String value = String.valueOf(entry.getValue());
                
                // 替换 ${key} 格式
                content = content.replace("${" + key + "}", value);
                // 替换 {key} 格式
                content = content.replace("{" + key + "}", value);
            }
        }
        
        return content;
    }

    /**
     * 判断响应是否成功
     * 
     * @param response 响应内容
     * @return 是否成功
     */
    private boolean isSuccess(String response) {
        if (StrUtil.isBlank(response)) {
            return false;
        }
        
        // 根据实际返回结果调整判断逻辑
        // 通常成功返回 "success" 或包含 "OK" 等标识
        return response.toLowerCase().contains("success") 
                || response.toLowerCase().contains("ok")
                || response.contains("0"); // 有些接口返回 0 表示成功
    }
}
