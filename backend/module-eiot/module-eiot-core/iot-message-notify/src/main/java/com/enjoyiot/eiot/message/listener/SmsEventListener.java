package com.enjoyiot.eiot.message.listener;


import com.enjoyiot.eiot.message.config.VertxManager;
import com.enjoyiot.eiot.message.event.MessageEvent;
import com.enjoyiot.module.system.api.sms.SmsSendApi;
import com.enjoyiot.module.system.api.sms.dto.send.SmsSendSingleToUserReqDTO;
import com.enjoyiot.module.eiot.api.alert.AlertApi;
import com.enjoyiot.module.eiot.api.alert.dto.Message;
import cn.hutool.core.util.StrUtil;
import io.vertx.ext.web.client.WebClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 短信事件监听器 - 通过系统短信模块发送短信
 *
 * 告警触发时，如果通道为 SMS，则通过系统短信管理模块发送短信。
 * 需要在通道模板中配置 smsTemplateCode 关联系统短信模板编码。
 *
 * 短信接收号码通过报警配置关联的消息推送岗位 + 当前值班人员解析：
 * ① 读取报警配置绑定的【消息推送岗位】，筛选同租户归属岗位且开启【消息推送】的用户；
 * ② 追加当前租户当前时段生效的值班人员；
 * ③ 人员集合去重后推送；无有效接收人则不推送
 *
 * @author EnjoyIot
 */
@Slf4j
@Component
public class SmsEventListener implements MessageEventListener {
    WebClient client = WebClient.create(VertxManager.INSTANCE.getVertx());

    @Resource
    private SmsSendApi smsSendApi;

    @Resource
    private AlertApi alertApi;

    @Override
    @EventListener(classes = MessageEvent.class, condition = "#event.message.channelCode=='SMS'")
    public void doEvent(MessageEvent event) {
        Message message = event.getMessage();

        // 校验短信模板编码
        String smsTemplateCode = message.getSmsTemplateCode();
        if (smsTemplateCode == null || smsTemplateCode.isEmpty()) {
            log.warn("[SmsEventListener] 通道模板未配置系统短信模板编码(smsTemplateCode)，跳过短信发送。alertConfigId: {}", message.getAlertConfigId());
            return;
        }

        Map<String, Object> templateParams = message.getParam() != null ? message.getParam() : Map.of();

        // 解析本次告警的推送接收人（消息推送岗位用户 + 当前值班人员）
        List<String> phones = resolvePhoneNumbers(message);
        if (phones.isEmpty()) {
            log.warn("[SmsEventListener] 本次告警无推送接收人，不执行短信推送，alertConfigId: {}, tenantId: {}, dn: {}",
                    message.getAlertConfigId(), message.getTenantId(), message.getDn());
            return;
        }

        // 逐个发送短信
        int successCount = 0;
        for (String phone : phones) {
            if (StrUtil.isBlank(phone)) {
                continue;
            }
            phone = phone.trim();
            try {
                SmsSendSingleToUserReqDTO reqDTO = new SmsSendSingleToUserReqDTO();
                reqDTO.setMobile(phone);
                reqDTO.setTemplateCode(smsTemplateCode);
                reqDTO.setTemplateParams(templateParams);
                smsSendApi.sendSingleSmsToAdmin(reqDTO);
                successCount++;
                log.info("[SmsEventListener] 短信发送成功，手机号: {}, 模板编码: {}", phone, smsTemplateCode);
            } catch (Exception e) {
                log.error("[SmsEventListener] 发送短信失败，手机号: {}, 模板编码: {}", phone, smsTemplateCode, e);
            }
        }
        log.info("[SmsEventListener] 短信发送完成，应发: {}, 成功: {}, 企业租户: {}",
                phones.size(), successCount, message.getTenantId());
    }

    /**
     * 解析本次告警的推送接收人手机号
     * 通过报警配置关联的消息推送岗位（同租户开启推送的用户）+ 当前值班人员获取，自动去重
     */
    private List<String> resolvePhoneNumbers(Message message) {
        List<String> phones = new ArrayList<>();

        if (message.getAlertConfigId() == null) {
            log.warn("[SmsEventListener] alertConfigId 为空，无法解析推送接收人");
            return phones;
        }

        try {
            List<String> resolved = alertApi.getAlertPushPhones(message.getAlertConfigId());
            if (resolved != null) {
                for (String p : resolved) {
                    if (StrUtil.isNotBlank(p) && !phones.contains(p.trim())) {
                        phones.add(p.trim());
                    }
                }
            }
            log.info("[SmsEventListener] 本次告警推送接收人，alertConfigId: {}, 手机号: {}",
                    message.getAlertConfigId(), phones);
        } catch (Exception e) {
            log.error("[SmsEventListener] 解析推送接收人失败，alertConfigId: {}", message.getAlertConfigId(), e);
        }

        return phones;
    }

}
