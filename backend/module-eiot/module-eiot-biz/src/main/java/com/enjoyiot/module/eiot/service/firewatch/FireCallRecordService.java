package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord.CallRecordPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.callrecord.CallRecordRespVO;
import org.springframework.core.io.Resource;

/**
 * 话单通话记录 Service
 * <p>
 * 系统租户可查全部（按租户/号码/方向筛选）；企业租户仅看本租户通话记录。
 *
 * @author EnjoyIot
 */
public interface FireCallRecordService {

    /**
     * 分页查询通话记录
     */
    PageResult<CallRecordRespVO> pageRecords(CallRecordPageReqVO pageReqVO);

    /**
     * 获取通话记录详情
     */
    CallRecordRespVO getRecord(Long id);

    /**
     * 代理下载录音文件
     *
     * @param id   通话记录ID
     * @param side 录音侧：mixed=混合 in=主叫 out=被叫
     * @return 录音文件资源
     */
    Resource downloadVoice(Long id, String side);
}
