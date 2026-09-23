package com.enjoyiot.module.system.framework.sms.core.client.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.enjoyiot.framework.common.core.KeyValue;
import com.enjoyiot.module.system.framework.sms.core.client.dto.SmsReceiveRespDTO;
import com.enjoyiot.module.system.framework.sms.core.client.dto.SmsSendRespDTO;
import com.enjoyiot.module.system.framework.sms.core.client.dto.SmsTemplateRespDTO;
import com.enjoyiot.module.system.framework.sms.core.enums.SmsTemplateAuditStatusEnum;
import com.enjoyiot.module.system.framework.sms.core.property.SmsChannelProperties;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 广州电力(东蓝信息)短信客户端实现
 *
 * 接口地址：http://sdk.gzdlinfo.cn/dlcpInterface/sdk/sendSms
 * 响应格式：XML，<error>0</error> 表示成功
 * apiKey 对应 cdkey，apiSecret 对应 password
 *
 * @author EnjoyIot
 */
@Slf4j
public class GzdlSmsClient extends AbstractSmsClient {

    private static final String SMS_SEND_URL = "http://sdk.gzdlinfo.cn/dlcpInterface/sdk/sendSms";
    private static final String RESPONSE_SUCCESS_CODE = "0";

    public GzdlSmsClient(SmsChannelProperties properties) {
        super(properties);
    }

    @Override
    public SmsSendRespDTO sendSms(Long logId, String mobile, String apiTemplateId,
                                  String content, List<KeyValue<String, Object>> templateParams) throws Throwable {
        // 构建短信内容：用 apiTemplateId 作为模板，替换参数
        String messageContent = content;
        if (templateParams != null) {
            for (KeyValue<String, Object> param : templateParams) {
                messageContent = messageContent.replace("${" + param.getKey() + "}", String.valueOf(param.getValue()));
            }
        }

        // 添加签名前缀
        messageContent = "【" + properties.getSignature() + "】" + messageContent;
 
        try {
            String encodedMessage = URLEncoder.encode(messageContent, StandardCharsets.UTF_8.name());
            String url = String.format("%s?cdkey=%s&password=%s&phone=%s&message=%s&addserial=&seqid=",
                    SMS_SEND_URL, properties.getApiKey(), properties.getApiSecret(), mobile, encodedMessage);

            log.info("[GZDL短信] 发送短信，手机号: {}, 内容: {}", mobile, messageContent);

            String xmlResponse = HttpUtil.get(url);
            log.info("[GZDL短信] 响应结果: {}", xmlResponse);

            boolean success = parseXmlSuccess(xmlResponse);

            SmsSendRespDTO resp = new SmsSendRespDTO();
            resp.setSuccess(success);
            resp.setApiRequestId(String.valueOf(logId));
            if (success) {
                resp.setApiCode(RESPONSE_SUCCESS_CODE);
                resp.setApiMsg("发送成功");
            } else {
                resp.setApiCode("1");
                resp.setApiMsg(xmlResponse);
            }
            return resp;
        } catch (Exception e) {
            log.error("[GZDL短信] 发送异常", e);
            SmsSendRespDTO resp = new SmsSendRespDTO();
            resp.setSuccess(false);
            resp.setApiCode("ERROR");
            resp.setApiMsg(e.getMessage());
            return resp;
        }
    }

    @Override
    public List<SmsReceiveRespDTO> parseSmsReceiveStatus(String text) {
        // GZDL短信平台暂不支持接收状态回调
        return List.of();
    }

    @Override
    public SmsTemplateRespDTO getSmsTemplate(String apiTemplateId) {
        // GZDL短信平台不需要预先创建模板，直接使用内容发送，默认审核通过
        SmsTemplateRespDTO dto = new SmsTemplateRespDTO();
        dto.setId(apiTemplateId);
        dto.setContent(apiTemplateId);
        dto.setAuditStatus(SmsTemplateAuditStatusEnum.SUCCESS.getStatus());
        return dto;
    }

    /**
     * 解析XML响应，判断是否成功
     */
    private boolean parseXmlSuccess(String xml) {
        if (StrUtil.isBlank(xml)) {
            return false;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            NodeList errorNodes = doc.getElementsByTagName("error");
            if (errorNodes.getLength() > 0) {
                String errorValue = errorNodes.item(0).getTextContent().trim();
                return RESPONSE_SUCCESS_CODE.equals(errorValue);
            }
            return false;
        } catch (Exception e) {
            log.error("[GZDL短信] 解析响应XML失败: {}", xml, e);
            return false;
        }
    }

}
