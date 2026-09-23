package com.enjoyiot.module.eiot.dal.dataobject.firewatch.enums;

// 触发事件
public enum AttendanceEvent {
    CLOCK_IN,           // 触发打卡
    SYSTEM_SETTLE,      // 系统日结/超时判定
    APPROVE_LEAVE       // 审批通过请假
}