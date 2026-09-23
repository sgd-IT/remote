package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 值班记录管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireDutyRecordService {

    /**
     * 创建记录
     *
     * @param createReqVO 创建信息
     * @return 记录ID
     */
    Long createRecord(FireDutyRecordCreateReqVO createReqVO);

    /**
     * 更新记录
     *
     * @param updateReqVO 更新信息
     */
    void updateRecord(FireDutyRecordUpdateReqVO updateReqVO);

    /**
     * 删除记录
     *
     * @param id 记录ID
     */
    void deleteRecord(Long id);

    /**
     * 获取记录详情
     *
     * @param id 记录ID
     * @return 记录详情
     */
    FireDutyRecordRespVO getRecord(Long id);

    /**
     * 分页查询记录列表
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<FireDutyRecordRespVO> pageRecords(FireDutyRecordPageReqVO pageReqVO);

    /**
     * 获取指定排班的记录列表
     *
     * @param scheduleId 排班ID
     * @return 记录列表
     */
    List<FireDutyRecordRespVO> listRecordsBySchedule(Long scheduleId);

    /**
     * 获取指定时间范围的记录
     *
     * @param propertyId 物业ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 记录列表
     */
    List<FireDutyRecordRespVO> listRecordsByTimeRange(Long propertyId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 创建交接班记录
     *
     * @param scheduleId 排班ID
     * @param handoverContent 交接内容
     * @param nextDutyUserId 下一班值班人ID
     * @return 记录ID
     */
    Long createHandoverRecord(Long scheduleId, String handoverContent, Long nextDutyUserId);

    /**
     * 创建巡查记录
     *
     * @param scheduleId 排班ID
     * @param content 巡查内容
     * @param status 状态
     * @param attachments 附件
     * @return 记录ID
     */
    Long createPatrolRecord(Long scheduleId, String content, String status, String attachments);

    /**
     * 获取记录统计
     *
     * @param propertyId 物业ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 统计结果
     */
    Map<String, Object> getRecordStatistics(Long propertyId, LocalDateTime startTime, LocalDateTime endTime);

}
