package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 火警处置中心 Service 接口
 * 
 * 功能概述：提供火警警情的接收、确认、处置全流程支持。
 *
 * @author EnjoyIot
 */
public interface FireAlarmOrderService {

    /**
     * 创建火警处置工单
     *
     * @param createReqVO 创建信息
     * @return 工单ID
     */
    Long createAlarmOrder(AlarmOrderCreateReqVO createReqVO);

    /**
     * 处置火警工单
     *
     * @param handleReqVO 处置信息
     */
    void handleAlarmOrder(AlarmOrderHandleReqVO handleReqVO);

    /**
     * 确认火警
     *
     * @param id 工单ID
     * @param handler 处理人
     * @param remark 备注
     */
    void confirmAlarm(Long id, String handler, String remark);

    /**
     * 标记为误报
     *
     * @param id 工单ID
     * @param handler 处理人
     * @param remark 备注
     */
    void markFalseAlarm(Long id, String handler, String remark);

    /**
     * 确认真实火警
     *
     * @param id 工单ID
     * @param handler 处理人
     * @param remark 备注
     */
    void confirmRealFire(Long id, String handler, String remark);

    /**
     * 关闭工单
     *
     * @param id 工单ID
     * @param handler 处理人
     * @param remark 备注
     */
    void closeOrder(Long id, String handler, String remark);

    /**
     * 复位设备
     *
     * @param id 工单ID
     */
    void resetDevice(Long id);

    /**
     * 消音
     *
     * @param id 工单ID
     */
    void muteAlarm(Long id);

    /**
     * 拨打电话
     *
     * @param id 工单ID
     * @param phone 电话号码
     */
    void makePhoneCall(Long id, String phone);

    /**
     * 发送通知
     *
     * @param id 工单ID
     * @param channels 通知渠道
     */
    void sendNotification(Long id, String[] channels);

    /**
     * 获取火警工单详情
     *
     * @param id 工单ID
     * @return 工单详情
     */
    AlarmOrderRespVO getAlarmOrder(Long id);

    /**
     * 分页查询火警工单
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<AlarmOrderRespVO> pageAlarmOrders(AlarmOrderPageReqVO pageReqVO);

    /**
     * 获取报警记录统计（全量聚合）
     *
     * @return 统计数据
     */
    AlarmRecordStatisticsRespVO getAlarmRecordStatistics();

    /**
     * 导出报警记录PDF
     *
     * @param deviceType 设备类型
     * @param alarmType 报警类型
     * @param status 处置状态
     * @param propertyId 物业ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param response HTTP响应
     */
    void exportAlarmOrderPdf(String deviceType, String alarmType, String status,
                             Long propertyId, String startTime, String endTime,
                             HttpServletResponse response) throws IOException;

}
