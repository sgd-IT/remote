package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 值班管理管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireDutyScheduleService {

    /**
     * 创建排班
     *
     * @param createReqVO 创建信息
     * @return 排班ID
     */
    Long createSchedule(FireDutyScheduleCreateReqVO createReqVO);

    /**
     * 批量创建排班
     *
     * @param createReqVOList 创建信息列表
     * @return 创建数量
     */
    Integer batchCreateSchedule(List<FireDutyScheduleCreateReqVO> createReqVOList);

    /**
     * 更新排班
     *
     * @param updateReqVO 更新信息
     */
    void updateSchedule(FireDutyScheduleUpdateReqVO updateReqVO);

    /**
     * 删除排班
     *
     * @param id 排班ID
     */
    void deleteSchedule(Long id);

    /**
     * 获取排班详情
     *
     * @param id 排班ID
     * @return 排班详情
     */
    FireDutyScheduleRespVO getSchedule(Long id);

    /**
     * 分页查询排班列表
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<FireDutyScheduleRespVO> pageSchedules(FireDutySchedulePageReqVO pageReqVO);

    /**
     * 获取指定日期范围的排班列表
     *
     * @param propertyId 物业ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 排班列表
     */
    List<FireDutyScheduleRespVO> listSchedulesByDateRange(Long propertyId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取指定日期的排班
     *
     * @param propertyId 物业ID
     * @param date 日期
     * @return 排班列表
     */
    List<FireDutyScheduleRespVO> listSchedulesByDate(Long propertyId, LocalDate date);

    /**
     * 获取当前值班人员
     *
     * @param propertyId 物业ID（可选）
     * @return 当前值班人员列表
     */
    List<FireDutyScheduleRespVO> getCurrentDuty(Long propertyId);

    /**
     * 签到
     *
     * @param id 排班ID
     * @param checkInType 签到方式
     */
    void checkIn(Long id, String checkInType);

    /**
     * 签退
     *
     * @param id 排班ID
     */
    void checkOut(Long id);

    /**
     * 换班
     *
     * @param id 原排班ID
     * @param newUserId 新值班人ID
     * @param reason 换班原因
     */
    void changeDuty(Long id, Long newUserId, String reason);

    /**
     * 获取排班统计
     *
     * @param propertyId 物业ID
     * @param startDate 开始日期（可为null，统计全部）
     * @param endDate   结束日期（可为null，统计全部）
     * @return 统计结果
     */
    Map<String, Object> getScheduleStatistics(Long propertyId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取某人某月的值班统计
     *
     * @param userId 用户ID
     * @param year 年份
     * @param month 月份
     * @return 值班天数
     */
    Integer getUserDutyDays(Long userId, Integer year, Integer month);

    /**
     * 自动排班
     *
     * @param propertyId 物业ID
     * @param year 年份
     * @param month 月份
     * @param dutyUsers 值班人员ID列表
     * @return 生成的排班数量
     */
    Integer autoSchedule(Long propertyId, Integer year, Integer month, List<Long> dutyUsers);

    /**
     * Excel导入排班
     *
     * @param file       Excel文件
     * @param propertyId 所属物业ID
     * @return 导入结果（successCount, failCount, errors）
     */
    Map<String, Object> importExcelSchedule(MultipartFile file, Long propertyId);
}
