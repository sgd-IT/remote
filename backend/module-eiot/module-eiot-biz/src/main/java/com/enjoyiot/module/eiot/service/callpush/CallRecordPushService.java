package com.enjoyiot.module.eiot.service.callpush;

import com.enjoyiot.module.eiot.controller.admin.callpush.vo.CallRecordPushReqVO;

/**
 * 话单推送接收 Service
 * <p>
 * 能力开放平台通过 HTTP POST + JSON 推送话单，本服务负责：
 * 1. 幂等校验（sessionid 唯一）
 * 2. 号码匹配：callerNbr 命中座机 → 呼出；calledNbr 命中座机 → 呼入
 * 3. 归属租户落库（未匹配 → 系统租户）
 *
 * @author EnjoyIot
 */
public interface CallRecordPushService {

    /**
     * 接收话单推送（异步处理，Controller 立即返回 success）
     *
     * @param reqVO 话单推送请求
     */
    void pushCallRecord(CallRecordPushReqVO reqVO);
}
