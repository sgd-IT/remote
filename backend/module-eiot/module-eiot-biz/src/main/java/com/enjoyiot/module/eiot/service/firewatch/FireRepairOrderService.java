package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderAssignReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.repair.RepairOrderUpdateReqVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireRepairOrderDO;

import java.util.List;
import java.util.Map;

/**
 * 维修工单管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireRepairOrderService {

    /**
     * 手动创建维修工单
     *
     * @param reqVO 创建信息
     * @return 工单ID
     */
    Long createRepairOrder(RepairOrderCreateReqVO reqVO);

    /**
     * 从故障告警生成维修工单
     *
     * @param alertRecordId 告警记录ID
     * @return 工单ID
     */
    Long createFromAlert(Long alertRecordId);

    /**
     * 更新工单
     *
     * @param reqVO 更新信息
     */
    void updateRepairOrder(RepairOrderUpdateReqVO reqVO);

    /**
     * 派单（指派给企业，自动填充企业联系人信息）
     *
     * @param reqVO 派单信息
     */
    void assignOrder(RepairOrderAssignReqVO reqVO);

    /**
     * 完成维修
     *
     * @param id 工单ID
     * @param completeRemark 完成备注
     * @param completeImages 完成图片
     */
    void completeOrder(Long id, String completeRemark, String completeImages);

    /**
     * 关闭工单
     *
     * @param id 工单ID
     */
    void closeOrder(Long id);

    /**
     * 获取工单详情
     *
     * @param id 工单ID
     * @return 工单详情
     */
    RepairOrderRespVO getRepairOrder(Long id);

    /**
     * 分页查询工单
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<RepairOrderRespVO> pageRepairOrders(RepairOrderPageReqVO pageReqVO);

    /**
     * 统计各状态工单数
     *
     * @return 各状态统计
     */
    Map<String, Integer> getOrderStatusStatistics();

    /**
     * 签单
     *
     * @param id 工单ID
     */
    void signOrder(Long id);

    /**
     * 开始维修
     *
     * @param id 工单ID
     */
    void startRepair(Long id);

    /**
     * 评价工单
     *
     * @param id 工单ID
     * @param satisfaction 满意度评分(1-5)
     * @param suggestion 评价建议
     */
    void rateOrder(Long id, Integer satisfaction, String suggestion);

    /**
     * 获取超期工单数
     *
     * @return 超期工单数
     */
    Integer getOverdueOrderCount();

    // ==================== 催办通知相关 ====================

    /**
     * 获取超期工单列表（管理员查全部，企业查自己的）
     */
    List<FireRepairOrderDO> getOverdueOrderList();

    /**
     * 获取尚未发送催办通知的超期工单列表
     */
    List<FireRepairOrderDO> getUnurgedOverdueOrderList();

    /**
     * 批量更新催办状态
     */
    void markUrgeSent(List<Long> orderIds);

    /**
     * 统计尚未催办的超期工单数
     */
    Integer countUnurgedOverdueOrders();

    /**
     * 发送催办通知（核心逻辑，同时支持手动和自动触发）
     *
     * @param orders 待催办的超期工单列表
     * @param plans 待催办的逾期计划列表
     * @return 发送结果统计
     */
    Map<String, Object> sendUrgeNotifications(List<FireRepairOrderDO> orders, List<FireMaintenancePlanDO> plans);

}
