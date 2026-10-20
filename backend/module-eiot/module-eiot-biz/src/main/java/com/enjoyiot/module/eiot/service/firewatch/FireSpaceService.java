package com.enjoyiot.module.eiot.service.firewatch;

import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceDTO;
import com.enjoyiot.module.eiot.controller.admin.firewatch.vo.space.FireSpaceUpdateDTO;
import com.enjoyiot.module.eiot.dal.dataobject.firewatch.FireSpaceDO;

import java.util.List;

/**
 * 空间管理 Service（楼栋/楼层/区域/详细位置）
 * <p>
 * 数据按当前租户隔离。
 *
 * @author sgd
 */
public interface FireSpaceService {

    /**
     * 创建空间（当前租户下 楼栋/楼层/区域/详细位置 同组合不可重复）
     */
    Long createFireSpace(FireSpaceDTO fireSpaceDTO);

    /**
     * 查询空间列表（不分页，按录入顺序）
     * <p>
     * 系统租户管理员：tenantId 为空查全部，否则查指定租户；企业租户：仅本租户
     *
     * @param tenantId 目标租户（仅系统租户管理员生效）
     */
    List<FireSpaceDO> listFireSpace(Long tenantId);

    /**
     * 更新空间（保持原租户不变；楼栋/楼层/区域/详细位置 同组合不可重复）
     */
    void updateFireSpace(FireSpaceUpdateDTO updateDTO);

    /**
     * 删除空间（存在设备引用时不允许删除）
     */
    void deleteFireSpace(Long id);
}
