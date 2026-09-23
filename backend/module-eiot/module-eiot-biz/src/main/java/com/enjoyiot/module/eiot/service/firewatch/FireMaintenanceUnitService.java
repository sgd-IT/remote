package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.framework.common.pojo.PageResult;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitCreateReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitPageReqVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitRespVO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.maintenanceunit.MaintenanceUnitUpdateReqVO;

import java.util.List;

/**
 * 维保单位管理 Service 接口
 *
 * @author EnjoyIot
 */
public interface FireMaintenanceUnitService {

    /**
     * 创建维保单位
     */
    Long createUnit(MaintenanceUnitCreateReqVO reqVO);

    /**
     * 更新维保单位
     */
    void updateUnit(MaintenanceUnitUpdateReqVO reqVO);

    /**
     * 删除维保单位
     */
    void deleteUnit(Long id);

    /**
     * 获取维保单位详情
     */
    MaintenanceUnitRespVO getUnit(Long id);

    /**
     * 分页查询维保单位
     */
    PageResult<MaintenanceUnitRespVO> pageUnits(MaintenanceUnitPageReqVO pageReqVO);

    /**
     * 获取维保单位列表（不分页，用于下拉选择）
     */
    List<MaintenanceUnitRespVO> listUnits();
}
