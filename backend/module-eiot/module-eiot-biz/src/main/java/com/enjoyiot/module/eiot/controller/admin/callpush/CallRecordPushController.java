package com.enjoyiot.module.eiot.controller.admin.callpush;

import com.enjoyiot.framework.common.util.json.JsonUtils;
import com.enjoyiot.module.eiot.controller.admin.callpush.vo.CallRecordPushReqVO;
import com.enjoyiot.module.eiot.service.callpush.CallRecordPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 话单推送接收 Controller
 * <p>
 * 能力开放平台 → 服务商（本系统），HTTP POST + JSON 推送话单，
 * 本系统返回 "success" 表示接收成功（参照《话单推送接口文档 V2.6》）。
 *
 * @author EnjoyIot
 */
@RestController
@Slf4j
@Tag(name = "能力开放平台 - 话单推送")
@RequestMapping("/eiot/callpush")
public class CallRecordPushController {

    @Resource
    private CallRecordPushService callRecordPushService;

    @PostMapping("/record")
    @Operation(summary = "话单推送接收", description = "接收能力开放平台推送的话单，返回 success 表示接收成功")
    public String pushRecord(@RequestBody(required = false) CallRecordPushReqVO rawBody) {
        log.info("[话单推送] 收到推送报文: {}", rawBody);
        if (rawBody == null) {
            return "success";
        }
        try {

            callRecordPushService.pushCallRecord(rawBody);
            
            // CallRecordPushReqVO reqVO = JsonUtils.parseObject(rawBody, CallRecordPushReqVO.class);
            // if (reqVO != null) {
            //     callRecordPushService.pushCallRecord(rawBody);
            // }
        } catch (Exception e) {
            log.error("[话单推送] 报文解析失败", e);
        }
        // 无论处理结果如何都返回 success，避免平台重复推送
        return "success";
    }
}
