package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 隐患管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireHazardService {

    /**
     * 创建隐患
     *
     * @param createReqVO 创建信息
     * @return 隐患ID
     */
    Long createHazard(FireHazardCreateReqVO createReqVO);

    /**
     * 更新隐患
     *
     * @param updateReqVO 更新信息
     */
    void updateHazard(FireHazardUpdateReqVO updateReqVO);

    /**
     * 删除隐患
     *
     * @param id 隐患ID
     */
    void deleteHazard(Long id);

    /**
     * 获取隐患详情
     *
     * @param id 隐患ID
     * @return 隐患详情
     */
    FireHazardRespVO getHazard(Long id);

    /**
     * 分页查询隐患列表
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<FireHazardRespVO> pageHazards(FireHazardPageReqVO pageReqVO);

    /**
     * 提交整改进度
     *
     * @param reqVO 整改信息
     */
    void submitRectification(FireHazardRectificationReqVO reqVO);

    /**
     * 验收复查
     *
     * @param id     隐患ID
     * @param result 验收结果: approved-通过, rejected-不通过
     * @param remark 验收意见
     */
    void reviewRectification(Long id, String result, String remark);

    /**
     * 关闭隐患
     *
     * @param id     隐患ID
     * @param remark 关闭原因
     */
    void closeHazard(Long id, String remark);

    /**
     * 获取隐患统计
     *
     * @param propertyId 物业ID
     * @param startDate  开始日期
     * @param endDate    结束日期
     * @return 统计结果
     */
    Map<String, Object> getHazardStatistics(Long propertyId, LocalDate startDate, LocalDate endDate);

    /**
     * 导入隐患
     *
     * @param list 导入的隐患列表
     * @return 导入结果（成功数、失败数、失败详情）
     */
    Map<String, Object> importHazards(List<FireHazardImportVO> list);

}
