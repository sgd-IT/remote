package com.enjoyiot.module.eiot.controller.admin.aep;

import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.controller.admin.aep.dto.AepPushMessage;
import com.enjoyiot.module.eiot.service.aep.AepCallbackService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/eiot/aep")
public class AepCallbackController {

    @Resource
    private AepCallbackService aepCallbackService;

    // ==================== 烟感 ====================

    /**
     * 烟感 - 产品级回调（设备增删、上下线）
     */
    @PostMapping("/product/smoke/callback")
    public String productSmokeCallback(@RequestBody(required = false) String rawBody) {
        log.info("[AEP回调] 烟感产品级, 原始报文: {}", rawBody);
        if (rawBody != null) {
            try {
                AepPushMessage message = JsonUtils.parseObject(rawBody, AepPushMessage.class);
                if (message != null) {
                    aepCallbackService.processPushMessage(message);
                }
            } catch (Exception e) {
                log.error("[AEP回调] 烟感产品级报文解析失败", e);
            }
        }
        return "success";
    }

    // /**
    //  * 烟感 - 设备级回调（数据变化、指令响应、事件上报）
    //  */
    // @PostMapping("/device/smoke/callback")
    // public String deviceSmokeCallback(@RequestBody(required = false) String rawBody) {
    //     log.info("[AEP回调] 烟感设备级, 原始报文: {}", rawBody);
    //     if (rawBody != null) {
    //         try {
    //             AepPushMessage message = JsonUtils.parseObject(rawBody, AepPushMessage.class);
    //             if (message != null) {
    //                 aepCallbackService.processPushMessage(message);
    //             }
    //         } catch (Exception e) {
    //             log.error("[AEP回调] 烟感设备级报文解析失败", e);
    //         }
    //     }
    //     return "success";
    // }
}
