package com.enjoyiot.module.eiot.api.alert.sms;

import cn.hutool.http.HttpUtil;
import cn.hutool.core.util.StrUtil;
import com.enjoyiot.framework.common.exception.util.ServiceExceptionUtil;
import com.enjoyiot.module.eiot.api.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
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
 * 东蓝信息短信发送工具类
 * 
 * 接口地址：http://sdk.gzdlinfo.cn/dlcpInterface/sdk/sendSms
 * 响应格式：XML，<error>0</error> 表示成功
 * 
 * 使用方式：
 * <pre>
 * @Resource
 * private GzdlSmsClient smsClient;
 * 
 * // 发送单条短信
 * smsClient.sendSms("13800138000", "火警通知：A栋3层发现火警，请立即处理！");
 * 
 * // 批量发送
 * smsClient.sendBatchSms(List.of("13800138000", "13900139000"), "火警通知...");
 * </pre>
 * 
 * @author EnjoyIot
 */
@Slf4j
@Component
public class GzdlSmsClient {

    /**
     * 短信发送接口URL
     */
    private static final String SMS_SEND_URL = "http://sdk.gzdlinfo.cn/dlcpInterface/sdk/sendSms";

    /**
     * 短信签名（默认自动添加）
     */
    private static final String DEFAULT_SIGNATURE = "【广东安为安全科技】";

    /**
     * 短信平台账号（可在配置文件中覆盖）
     */
    @Value("${sms.gzdl.cdkey:GZDLNQ0MS3749018276}")
    private String cdkey;

    /**
     * 短信平台密码（可在配置文件中覆盖）
     */
    @Value("${sms.gzdl.password:**}")
    private String password;

    /**
     * 发送单条短信
     * 
     * @param phone 手机号
     * @param message 短信内容（会自动添加签名）
     * @return 是否发送成功
     */
    public boolean sendSms(String phone, String message) {
        if (StrUtil.isBlank(phone)) {
            log.error("[GZDL短信平台] 手机号不能为空");
            return false;
        }

        if (StrUtil.isBlank(message)) {
            log.error("[GZDL短信平台] 短信内容不能为空");
            return false;
        }

        // 自动添加签名
        if (!message.startsWith("【")) {
            message = DEFAULT_SIGNATURE + message;
        }

        try {
            // URL编码
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.name());

            // 构建请求URL
            String url = String.format("%s?cdkey=%s&password=%s&phone=%s&message=%s&addserial=&seqid=",
                    SMS_SEND_URL, cdkey, password, phone, encodedMessage);

            log.info("[GZDL短信平台] 发送短信，手机号: {}, 内容: {}", phone, message);

            // 发送请求
            String xmlResponse = HttpUtil.get(url);

            log.info("[GZDL短信平台] 响应结果: {}", xmlResponse);

            // 解析XML响应
            boolean success = isSuccess(xmlResponse);
            
            if (!success) {
                log.error("[GZDL短信平台] 短信发送失败: {}", xmlResponse);
            } else {
                log.info("[GZDL短信平台] 短信发送成功");
            }
            
            return success;
        } catch (Exception e) {
            log.error("[GZDL短信平台] 短信发送异常", e);
            return false;
        }
    }

    /**
     * 批量发送短信
     * 
     * @param phones 手机号列表
     * @param message 短信内容
     * @return 成功发送的数量
     */
    public int sendBatchSms(List<String> phones, String message) {
        if (phones == null || phones.isEmpty()) {
            return 0;
        }

        int successCount = 0;
        for (String phone : phones) {
            if (sendSms(phone, message)) {
                successCount++;
            }
        }
        
        log.info("[GZDL短信平台] 批量发送完成，总数: {}, 成功: {}", phones.size(), successCount);
        return successCount;
    }

    /**
     * 发送短信（带格式化参数）
     * 
     * @param phone 手机号
     * @param template 短信模板（包含占位符，如：火警通知：{location}发现火警）
     * @param params 参数（如：Map.of("location", "A栋3层")）
     * @return 是否发送成功
     */
    public boolean sendSmsWithTemplate(String phone, String template, java.util.Map<String, String> params) {
        if (StrUtil.isBlank(template)) {
            return false;
        }

        String message = template;
        if (params != null) {
            // 替换占位符
            for (java.util.Map.Entry<String, String> entry : params.entrySet()) {
                message = message.replace("{" + entry.getKey() + "}", entry.getValue());
                message = message.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }

        return sendSms(phone, message);
    }

    /**
     * 解析XML响应，判断是否成功
     * 
     * 响应格式：
     * <response>
     *   <error>0</error>
     *   <message>成功</message>
     * </response>
     * 
     * @param xml XML响应内容
     * @return 是否成功（error=0表示成功）
     */
    public static boolean isSuccess(String xml) {
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
                return "0".equals(errorValue);
            }
            return false;
        } catch (Exception e) {
            log.error("[GZDL短信平台] 解析响应XML失败: {}", xml, e);
            return false;
        }
    }

    /**
     * 更新配置（如果需要动态修改 cdkey 或 password）
     */
    public void setCdkey(String cdkey) {
        this.cdkey = cdkey;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
