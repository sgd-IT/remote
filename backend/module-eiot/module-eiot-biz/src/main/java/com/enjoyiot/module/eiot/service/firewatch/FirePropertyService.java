package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.*;

import java.util.List;

/**
 * 物业单位管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FirePropertyService {

    /**
     * 创建物业
     *
     * @param createReqVO 创建信息
     * @return 物业ID
     */
    Long createProperty(FirePropertyCreateReqVO createReqVO);

    /**
     * 更新物业
     *
     * @param updateReqVO 更新信息
     */
    void updateProperty(FirePropertyUpdateReqVO updateReqVO);

    /**
     * 删除物业
     *
     * @param id 物业ID
     */
    void deleteProperty(Long id);

    /**
     * 获取物业详情
     *
     * @param id 物业ID
     * @return 物业详情
     */
    FirePropertyRespVO getProperty(Long id);

    /**
     * 分页查询物业列表
     *
     * @param pageReqVO 分页查询条件
     * @return 分页结果
     */
    PageResult<FirePropertyRespVO> pageProperties(FirePropertyPageReqVO pageReqVO);

    /**
     * 获取物业列表（不分页）
     *
     * @param status 状态（可选）
     * @return 物业列表
     */
    List<FirePropertyRespVO> listProperties(Integer status);

    /**
     * 更新物业状态
     *
     * @param id 物业ID
     * @param status 状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 获取物业统计信息
     *
     * @param id 物业ID
     * @return 统计信息
     */
    FirePropertyRespVO getPropertyStatistics(Long id);

    /**
     * 根据租户ID获取企业信息
     *
     * @param tenantId 租户ID
     * @return 企业信息
     */
    FirePropertyRespVO getPropertyByTenantId(Long tenantId);

}
