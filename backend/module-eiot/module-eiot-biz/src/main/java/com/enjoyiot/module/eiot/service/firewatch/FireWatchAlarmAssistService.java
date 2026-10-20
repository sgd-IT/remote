package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.assist.AlarmAssistRespVO;

/**
 * 告警辅助判断 Service（报警弹窗按 alarmId 查同区域设备与视频资源）
 *
 * @author sgd
 */
public interface FireWatchAlarmAssistService {

    /**
     * 按告警ID查询辅助判断信息；告警不存在返回 null
     */
    AlarmAssistRespVO getAlarmAssist(Long alarmId);
}
