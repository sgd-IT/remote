package com.enjoyiot.module.eiot.dal.dataobject.firewatch.enums;

public enum FireDutyScheduleState {
    NOT_CHECKED_IN,   // 未打卡
    CHECKED_IN_ON_TIME, // 正常签到
    CHECKED_IN_LATE,    // 迟到签到
    ABSENT              // 缺勤
}