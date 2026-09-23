package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanUpdateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.plan.PlanRespVO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireMaintenancePlanDO;

import java.util.List;

/**
 * 维保计划管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireMaintenancePlanService {

    /**
     * 创建维保计划
     *
     * @param reqVO 创建信息
     * @return 计划ID
     */
    Long createPlan(PlanCreateReqVO reqVO);

    /**
     * 更新维保计划
     *
     * @param reqVO 更新信息
     */
    void updatePlan(PlanUpdateReqVO reqVO);

    /**
     * 删除维保计划
     *
     * @param id 计划ID
     */
    void deletePlan(Long id);

    /**
     * 获取维保计划详情
     *
     * @param id 计划ID
     * @return 计划详情
     */
    PlanRespVO getPlan(Long id);

    /**
     * 分页查询维保计划
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<PlanRespVO> pagePlans(PlanPageReqVO pageReqVO);

    /**
     * 完成维保计划
     *
     * @param id 计划ID
     */
    void completePlan(Long id);

    /**
     * 获取已逾期的维保计划列表
     *
     * @return 逾期计划列表
     */
    List<PlanRespVO> getOverduePlans();

    /**
     * 统计已逾期的维保计划数
     *
     * @return 逾期计划数
     */
    Integer countOverdue();

    // ==================== 催办通知相关 ====================

    /**
     * 获取逾期计划列表（管理员查全部，企业查自己的）
     */
    List<FireMaintenancePlanDO> getOverduePlanList();

    /**
     * 获取尚未发送催办通知的逾期计划列表
     */
    List<FireMaintenancePlanDO> getUnurgedOverduePlanList();

    /**
     * 批量更新催办状态
     */
    void markUrgeSent(List<Long> planIds);

    /**
     * 统计尚未催办的逾期计划数
     */
    Integer countUnurgedOverduePlans();
}
